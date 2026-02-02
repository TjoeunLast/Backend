package com.example.project.security.auth;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // Added
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService service;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request
  ) {
    try {
      return ResponseEntity.ok(service.register(request));
    } catch (DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            AuthenticationResponse.builder().error(ex.getMessage()).build()
        );
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(
            AuthenticationResponse.builder().error("이미 가입된 이메일 주소입니다. 로그인을 시도해주세요.").build()
        );
    }
  }
  
  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request
  ) {
    try {
        return ResponseEntity.ok(service.authenticate(request));
    } catch (AuthenticationException ex) { // Catch AuthenticationException for bad credentials
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            AuthenticationResponse.builder().error("이메일 또는 비밀번호가 올바르지 않습니다.").build()
        );
    } catch (Exception e) { // Catch any other unexpected exceptions
        return ResponseEntity.badRequest().body(
            AuthenticationResponse.builder().error("로그인 요청에 실패했습니다.").build()
        );
    }
  }

  @PostMapping("/refresh-token")
  public void refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    service.refreshToken(request, response);
  }

  @GetMapping("/demo-controller")
  public ResponseEntity<String> sayHello() {
    return ResponseEntity.ok("인증 성공! 이 메시지는 보안 토큰이 있어야만 보입니다.");
  }
  
//[추가] 관리자용 빠른 유저 생성
 @PostMapping("/admin/create")
 public ResponseEntity<AuthenticationResponse> adminCreateUser(
     @RequestBody AuthenticationRequest request
 ) {
   try {
     // 서비스에 구현된 quickRegister 호출
     return ResponseEntity.ok(service.quickRegister(request));
   } catch (Exception e) {
     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
         AuthenticationResponse.builder()
             .error("관리자 권한 유저 생성 실패: " + e.getMessage())
             .build()
     );
   }
 }

}
