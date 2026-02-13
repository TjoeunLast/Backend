package com.example.project.member.dto;

import java.time.LocalDate;

import com.example.project.member.domain.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminUserListResponse {

    private String role;
    private String nickname;
    private String phone;
    private String email;
    private LocalDate enrolldate;

    public static AdminUserListResponse from(Users user) {
        return new AdminUserListResponse(
                user.getRole() != null ? user.getRole().name() : null,
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getEnrolldate()
        );
    }
}
