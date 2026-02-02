package com.example.project.member.dto;

import com.example.project.member.domain.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    
    // Neighborhood 정보 추가

    // 엔티티를 DTO로 변환하는 정적 메서드 (팩토리 메서드 패턴)
    public static UserResponseDto from(Users user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname() != null ? user.getNickname() : "")
                .profileImageUrl(user.getProfileImage() != null ? user.getProfileImage().getImageUrl() : "")
                .build();
    }
}
