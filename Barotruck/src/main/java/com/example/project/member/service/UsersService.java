package com.example.project.member.service;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.global.image.ImageUploadResponse;
import com.example.project.global.image.S3ImageService;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.UserResponseDto;
import com.example.project.member.repository.DriverRepository;
import com.example.project.member.repository.ShipperRepository;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository repository;
    private final ShipperRepository shipperRepository;
    private final DriverRepository driverRepository;
    private final S3ImageService s3ImageService;

    /**
     * 닉네임 중복 여부 확인
     * @return 중복이면 true, 사용 가능하면 false
     */
    public boolean isNicknameDuplicated(String nickname) {
        return repository.existsByNickname(nickname);
    }
    
    // ================================
    // ❗ 3) 회원 탈퇴 (soft delete)
    // ================================
    public void deleteUser(Principal principal) {
        Users user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        user.setDelflag("A");
        user.setDeletedate(LocalDate.now());
        repository.save(user);
    }

    // ================================
    // ❗ 4) 회원 복구
    // ================================
    public void restoreUser(Principal principal) {
        Users user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        if ("A".equals(user.getDelflag())) {
            throw new IllegalStateException("관리자에 의해 탈퇴된 계정은 복구할 수 없습니다.");
        }

        user.setDelflag("N");
        user.setDeletedate(null);
        repository.save(user);
    }

    // ================================
    // ❗❗❗ 수정 금지 — 원본 그대로 유지 ❗❗❗
    // ================================
    // 비번 변경
    
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {

        var user = (Users) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }
        // check if the two new passwords are the same
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Password are not the same");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // save the new password
        repository.save(user);
    }
    
    
    /**
     * 사용자의 FCM 토큰을 데이터베이스에 반영하는 서비스 메서드
     * @Transactional: 이 메서드 안의 작업이 하나라도 실패하면 DB 수정을 없던 일로 되돌립니다(Rollback).
     * 작업이 성공적으로 끝나면 자동으로 DB에 변경사항을 저장(Commit)합니다.
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        // 1. 전달받은 유저 ID를 이용해 DB에서 해당 사용자를 찾습니다.
        // .orElseThrow: 만약 해당 ID의 사용자가 DB에 없으면 예외(에러)를 발생시키고 로직을 중단합니다.
        Users user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));
                
        // 2. 조회된 유저 엔티티 객체의 fcmToken 필드 값을 클라이언트가 보내준 새 값으로 수정합니다.
        // JPA의 '더티 체킹(Dirty Checking)' 기능 덕분에, 필드 값만 바꿔도 트랜잭션 종료 시 DB에 자동으로 반영됩니다.
        user.updateFcmToken(fcmToken); 
    }


	
	// [Create & Update] 프로필 이미지 등록/수정 (하나로 해결)
    @Transactional
    public void uploadProfileImage(Long userId, MultipartFile file) {
        Users user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        // 1. 기존 이미지가 이미 있으면 S3에서 먼저 삭제 (찌꺼기 제거)
        if (user.getProfileImage() != null) {
            s3ImageService.delete(user.getProfileImage().getS3Key());
        }

        // 2. 새 이미지 S3 업로드
        ImageUploadResponse res = s3ImageService.upload(file, "profiles");

        // 3. 유저 엔티티 필드 업데이트 (DB 저장)
        user.updateProfileImage(res);
    }

    // [Read] 프로필 이미지 경로 조회
    @Transactional(readOnly = true)
    public String getProfileImageUrl(Long userId) {
        Users user = repository.findById(userId).orElseThrow();
        return (user.getProfileImage() != null) ? user.getProfileImage().getImageUrl() : "기본이미지URL";
    }

    // [Delete] 프로필 이미지 삭제 (기본 이미지로 돌아가기)
    @Transactional
    public void deleteProfileImage(Long userId) {
        Users user = repository.findById(userId).orElseThrow();

        if (user.getProfileImage() != null) {
            // 1. S3 실제 파일 삭제
            s3ImageService.delete(user.getProfileImage().getS3Key());
            // 2. 엔티티 필드 비우기
            user.clearProfileImage();
        }
    }
    
 // 유저 정보 조회 로직 (Neighborhood 정보 포함)
    @Transactional(readOnly = true)
    public UserResponseDto getUserInfo(Long userId) {
        Users user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));



        // 프론트엔드에 필요한 정보만 골라서 DTO로 변환
        return UserResponseDto.from(user);
    }

    
    
    
    
    
    
    
    

}
