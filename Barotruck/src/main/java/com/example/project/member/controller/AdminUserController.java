package com.example.project.member.controller;

import java.util.List;

import com.example.project.member.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.member.dto.AdminUserListResponse;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserListResponse>> listUsers(
            @AuthenticationPrincipal Users currentUser,
            @RequestParam(name = "role", required = false) Role role) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(adminUserService.getAdminUserList(role));
    }
}
