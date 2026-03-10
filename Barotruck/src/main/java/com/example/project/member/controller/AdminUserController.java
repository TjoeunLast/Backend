package com.example.project.member.controller;

import java.util.List;

import com.example.project.member.service.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.member.dto.AdminUserResponse;
import com.example.project.member.domain.Users;
import com.example.project.global.api.PaginationUtils;
import com.example.project.security.user.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<?> listUsers(
            @AuthenticationPrincipal Users currentUser,
            @RequestParam(name = "role", required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable,
            NativeWebRequest webRequest) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        List<AdminUserResponse> users = adminUserService.getAdminUserList(role);
        if (!PaginationUtils.isPagedRequest(webRequest)) {
            return ResponseEntity.ok(users);
        }
        return ResponseEntity.ok(PaginationUtils.paginate(users, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse.AdminUserDetailResponse> getUserDetail(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable("userId") Long userId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(adminUserService.getAdminUserDetail(userId));
    }

    @PostMapping("/delete/{userId}")
    public ResponseEntity<String> deleteUser(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable("userId") Long userId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        adminUserService.deleteUserById(userId);
        return ResponseEntity.ok("삭제 완료");
    }

    @PostMapping("/restore/{userId}")
    public ResponseEntity<String> restoreUser(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable("userId") Long userId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        adminUserService.restoreUserById(userId);
        return ResponseEntity.ok("복구 완료");
    }
}
