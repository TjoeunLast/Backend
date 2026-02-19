package com.example.project.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.project.global.image.ImageInfo;
import com.example.project.member.domain.Users;

import lombok.*;

@Getter
@AllArgsConstructor
public class AdminUserResponse {

    private String role;
    private String nickname;
    private String phone;
    private String email;
    private LocalDate enrolldate;

    public static AdminUserResponse from(Users user) {
        return new AdminUserResponse(
                user.getRole() != null ? user.getRole().name() : null,
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getEnrolldate()
        );
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDetailResponse {
        private Long userId;
        private Integer age;
        private LocalDate deletedate;
        private String delflag;
        private String email;
        private LocalDate enrolldate;
        private String gender;
        private String nickname;
        private String password;
        private String phone;
        private String imageUrl;
        private String originalName;
        private Long ratingAvg;
        private String regflag;
        private String isOwner;
        private LocalDateTime rate;
        private Long userLevel;

        public static AdminUserDetailResponse from(com.example.project.member.domain.Users user) {
            ImageInfo imageInfo = user.getProfileImage();

            return new AdminUserDetailResponse(
                    user.getUserId(),
                    user.getAge(),
                    user.getDeletedate(),
                    user.getDelflag(),
                    user.getEmail(),
                    user.getEnrolldate(),
                    user.getGender(),
                    user.getNickname(),
                    null, // password는 내려주지 말기
                    user.getPhone(),
                    imageInfo != null ? imageInfo.getImageUrl() : null,
                    imageInfo != null ? imageInfo.getOriginalName() : null,
                    user.getRatingAvg(),
                    user.getRegflag(),
                    user.getRole() != null ? user.getRole().name() : null,
                    user.getRate(),
                    user.getUser_level()
            );
        }

    }
}
