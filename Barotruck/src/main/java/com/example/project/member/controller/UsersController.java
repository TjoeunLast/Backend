package com.example.project.member.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.member.domain.Users;
import com.example.project.member.dto.UserResponseDto;
import com.example.project.member.service.UsersService;
import com.example.project.security.user.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService service;

    // =========================
    // 3) 비밀번호 변경
    // =========================
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest req,
            Principal principal) {

        service.changePassword(req, principal);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

    // =========================
    // 4) 회원 탈퇴
    // =========================
    @PostMapping("/delete")
    public ResponseEntity<String> deleteUser(Principal principal) {
        service.deleteUser(principal);
        return ResponseEntity.ok("회원 탈퇴 처리되었습니다.");
    }

    // =========================
    // 5) 회원 복구
    // =========================
    @PostMapping("/restore")
    public ResponseEntity<String> restoreUser(Principal principal) {
        service.restoreUser(principal);
        return ResponseEntity.ok("회원 상태가 복구되었습니다.");
    }

    /**
     * 닉네임 중복 체크 API
     * GET /api/members/check-nickname?nickname=사용자이름
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(
            @RequestParam("nickname") String nickname) {
        boolean isDuplicated = service.isNicknameDuplicated(nickname);
        // JSON 형태로 반환: {"isDuplicated": true/false}
        return ResponseEntity.ok(Map.of("isDuplicated", isDuplicated));
    }

    /**
     * FCM 토큰 등록 및 업데이트를 위한 컨트롤러 메서드
     * 
     * @AuthenticationPrincipal: 인증된 사용자의 정보(세션/JWT)를 자동으로 주입받습니다.
     * @RequestBody: 클라이언트가 보낸 JSON 데이터 {"fcmToken": "..."}를 Map 형태로 읽어옵니다.
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal Users userDetails, // 1. 현재 로그인한 유저의 정보를 가져옵니다.
            @RequestBody Map<String, String> body // 2. 프론트엔드가 보낸 JSON 본문을 Map 객체로 변환하여 받습니다.
    ) {
        // 3. Map 객체 안에서 "fcmToken"이라는 Key를 가진 값(문자열 토큰)을 추출합니다.
        String fcmToken = body.get("fcmToken");

        // 4. 서비스 계층으로 유저의 고유 ID(PK)와 추출한 토큰 값을 넘겨 로직을 처리하게 합니다.
        service.updateFcmToken(userDetails.getUserId(), fcmToken);

        // 5. 작업이 성공적으로 끝났음을 알리는 200 OK 상태 코드를 반환합니다.
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin-force-allocate-blocked")
    public ResponseEntity<Void> updateAdminForceAllocateBlocked(
            @AuthenticationPrincipal Users userDetails,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean blocked = Boolean.TRUE.equals(body.get("blocked"));
        service.updateAdminForceAllocateBlocked(userDetails.getUserId(), blocked);
        return ResponseEntity.ok().build();
    }

    // =========================
    // 6) 유저 정보 조회 (프로필 포함)
    // =========================
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(
            @AuthenticationPrincipal Users user) {
        // 서비스에서 DTO나 Map으로 변환하여 가져옵니다.
        UserResponseDto userInfo = service.getUserInfo(user.getUserId());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 프로필 이미지 등록 및 수정
     * POST /api/v1/users/me/image
     * form-data key: "image"
     */
    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(
            @AuthenticationPrincipal Users user,
            @RequestParam("image") MultipartFile file) {
        String imageUrl = service.uploadProfileImage(user.getUserId(), file);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 프로필 이미지 조회
     * GET /api/v1/users/me/image
     */
    @GetMapping("/me/image")
    public ResponseEntity<String> getProfileImage(@AuthenticationPrincipal Users user) {
        String imageUrl = service.getProfileImageUrl(user.getUserId());
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 프로필 이미지 삭제
     * DELETE /api/v1/users/me/image
     */
    @DeleteMapping("/me/image")
    public ResponseEntity<String> deleteProfileImage(@AuthenticationPrincipal Users user) {
    	service.deleteProfileImage(user.getUserId());
        return ResponseEntity.ok("프로필 이미지가 삭제되었습니다.");
    }
    
    /**
     * 유저 정보 수정
     * PATCH /api/user/me
     */
    @PatchMapping("/me")
    public ResponseEntity<String> updateMyInfo(
            @AuthenticationPrincipal Users user,
            @RequestBody UserResponseDto updateDto) {
        
        service.updateMyInfo(user.getUserId(), updateDto);
        return ResponseEntity.ok("정보가 성공적으로 수정되었습니다.");
    }

    /**
     * 회원 탈퇴
     * DELETE /api/user/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> withdraw(
            @AuthenticationPrincipal Users user) {
        
    	String result = service.deleteUser(user.getUserId());
        return ResponseEntity.ok(result);
    }
}
