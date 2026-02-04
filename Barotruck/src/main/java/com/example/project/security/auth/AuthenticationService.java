package com.example.project.security.auth;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Shipper;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.DriverRepository;
import com.example.project.member.repository.ShipperRepository;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.config.JwtService;
import com.example.project.security.token.Token;
import com.example.project.security.token.TokenRepository;
import com.example.project.security.token.TokenType;
import com.example.project.security.user.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UsersRepository repository;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final RestTemplate restTemplate; // RestTemplate을 빈으로 주입받아 사용
	private final ShipperRepository shipperRepository;
	private final DriverRepository driversRepository;
	/**
	 * 회원가입 처리
	 */
	public AuthenticationResponse register(RegisterRequest request) {

		// 전달받은 neighborhoodId로 동네 정보 조회
		if(request.getRole() == Role.ADMIN) {
			return null;
		}
		// 회원 엔티티 생성
		var user = Users.builder()
				.nickname(request.getNickname())
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.gender(request.getGender())
				.age(request.getAge())
				.phone(request.getPhone())
				.role(request.getRole())
				.build();
		
		// 2. DTO가 존재하는 쪽을 선택하여 저장
	    if (request.getShipper() != null) {
	        ShipperDto sDto = request.getShipper();
	        Shipper shipper = Shipper.builder()
	                .companyName(sDto.getCompanyName())
	                .bizRegNum(sDto.getBizRegNum())
	                .user(user)
	                .build();
	        shipperRepository.save(shipper);
	    } 
	    else if (request.getDriver() != null) {
	        DriverDto dDto = request.getDriver();
	        Driver driver = Driver.builder()
	                .carNum(dDto.getCarNum())
	                .carType(dDto.getCarType())
	                .user(user)
	                .build();
	        driversRepository.save(driver);
	    }
		
		

		try {
			// 사용자 저장 (여기서 유니크 제약조건 위반 시 예외 발생 가능)
			var savedUser = repository.save(user);

			// JWT 토큰 및 리프레시 토큰 생성
			var jwtToken = jwtService.generateToken(user);
			var refreshToken = jwtService.generateRefreshToken(user);

			// 사용자 토큰 저장
			saveUserToken(savedUser, jwtToken);

			return AuthenticationResponse.builder()
					.accessToken(jwtToken)
					.refreshToken(refreshToken)
					.userId(user.getUserId()) // DB에 저장된 user의 ID 반환
					.build();

		} catch (DataIntegrityViolationException e) {

			// DB 예외의 실제 원인이 SQL 예외인지 확인 (Oracle ORA-00001 등)
			Throwable rootCause = e.getRootCause();

			if (rootCause instanceof SQLException) {
				SQLException sqlEx = (SQLException) rootCause;

				// Oracle의 UNIQUE 제약조건 위반 에러 코드는 1
				if (sqlEx.getErrorCode() == 1) {
					throw e;
				}
				// 에러 메시지에 EMAIL 관련 제약조건이 포함된 경우
				// (예: SPRINGBOOT.SYS_C008889 → 이메일 유니크 제약)
				if (sqlEx.getMessage() != null && sqlEx.getMessage().contains("EMAIL")) {
					throw new DuplicateEmailException("이미 가입된 이메일 주소입니다.");
				}
			}

			// 이메일 중복이 아닌 다른 무결성 오류라면 그대로 예외 재발생
			throw e;
		}
	}

	/**
	 * 일반 로그인 처리 (이메일 + 비밀번호)
	 */
	public AuthenticationResponse authenticate(AuthenticationRequest request) {

		// Spring Security 인증 처리
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		// 인증 성공 시 사용자 조회
		var user = repository.findByEmail(request.getEmail()).orElseThrow();

		// JWT 및 Refresh Token 생성
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);

		// 기존 토큰 모두 폐기
		revokeAllUserTokens(user);

		// 새 토큰 저장
		saveUserToken(user, jwtToken);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.userId(user.getUserId()) // 로그인 성공한 사용자 ID 반환
				.build();
	}

	/**
	 * 사용자 토큰 저장
	 */
	private void saveUserToken(Users user, String jwtToken) {
		var token = Token.builder().user(user).token(jwtToken).tokenType(TokenType.BEARER).expired(false).revoked(false)
				.build();
		tokenRepository.save(token);
	}

	/**
	 * 해당 사용자의 모든 기존 토큰을 만료 처리
	 */
	private void revokeAllUserTokens(Users user) {
		var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUserId());

		if (validUserTokens.isEmpty())
			return;

		validUserTokens.forEach(token -> {
			token.setExpired(true);
			token.setRevoked(true);
		});

		tokenRepository.saveAll(validUserTokens);
	}

	/**
	 * Refresh Token을 이용해 Access Token 재발급
	 */
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;

		// Authorization 헤더가 없거나 Bearer 토큰이 아니면 종료
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return;
		}

		refreshToken = authHeader.substring(7);
		userEmail = jwtService.extractUsername(refreshToken);

		if (userEmail != null) {
			var user = repository.findByEmail(userEmail).orElseThrow();

			// Refresh Token 유효성 검증
			if (jwtService.isTokenValid(refreshToken, user)) {

				// 새로운 Access Token 발급
				var accessToken = jwtService.generateToken(user);

				// 기존 토큰 폐기 후 새 토큰 저장
				revokeAllUserTokens(user);
				saveUserToken(user, accessToken);

				var authResponse = AuthenticationResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
						.build();

				// 응답으로 토큰 반환
				new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
			}
		}
	}


	public AuthenticationResponse quickRegister(AuthenticationRequest request) {
		
		
	    // 2. 회원 엔티티 생성 (필수 필드들은 임시값으로 채움)
	    var user = Users.builder()
	    		.email(request.getEmail())
	            .password(passwordEncoder.encode(request.getPassword()))
	            .nickname("관리자생성_" + System.currentTimeMillis()) // 중복 방지
	            .gender("F")            
	            .age(0)                   // 하드코딩: 0세
	            .build();

	    
	    
	    try {
	        // 3. 사용자 저장
	        var savedUser = repository.save(user);

	        // 4. JWT 토큰 및 리프레시 토큰 생성 (로그인 세션 유지용)
	        var jwtToken = jwtService.generateToken(savedUser);
	        var refreshToken = jwtService.generateRefreshToken(savedUser);

	        // 5. 토큰 저장 (기존 saveUserToken 메서드 활용)
	        saveUserToken(savedUser, jwtToken);

	        return AuthenticationResponse.builder()
	                .accessToken(jwtToken)
	                .refreshToken(refreshToken)
	                .userId(savedUser.getUserId())
	                .build();

	    } catch (DataIntegrityViolationException e) {
	        // 이메일 중복 체크 로직 (기존 register와 동일)
	        Throwable rootCause = e.getRootCause();
	        if (rootCause instanceof SQLException) {
	            SQLException sqlEx = (SQLException) rootCause;
	            if (sqlEx.getErrorCode() == 1 || (sqlEx.getMessage() != null && sqlEx.getMessage().contains("EMAIL"))) {
	                throw new DuplicateEmailException("이미 가입된 이메일 주소입니다.");
	            }
	        }
	        throw e;
	    }
	}
	

}
