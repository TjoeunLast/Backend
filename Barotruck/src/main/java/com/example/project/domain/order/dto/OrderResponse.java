package com.example.project.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.project.global.image.ImageInfo;
import com.example.project.member.domain.Driver;
import com.example.project.security.user.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    // 1. 주문 기본 정보
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;

    // 2. 상차지 정보
    private String startAddr;
    private String startType;
    private String startSchedule;
    private Long startNeighborhoodId;

    // 3. 하차지 정보
    private String endAddr;
    private String endType;
    private String endSchedule;
    private Long endNeighborhoodId;

    // 4. 화물 및 작업 정보
    private String cargoContent;
    private String loadMethod;
    private String workType;
    private BigDecimal tonnage;
    private String reqCarType;
    private String reqTonnage;
    private String driveMode;
    private Long loadWeight;

    // 5. 요금 정보
    private Long basePrice;
    private Long laborFee;
    private String payMethod;
    private Long feeRate;
    private Long totalPrice;

    // 6. 배차 정보
    private Long driverNo;

    // 7. 화주(사용자) 정보 - 맨 아래로 분리
    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long userId;
        private String email;
        private String phone;
        private String nickname;
        private ImageInfo profileImage;
        private Long ratingAvg;
        private Integer age;
        private Role role;
        private Driver driver; // 필요 시 포함하되, 무한 참조 방지를 위해 주의 필요

        // Users 엔티티를 입력받아 UserSummary DTO로 변환하는 메서드
        public static UserSummary from(com.example.project.member.domain.Users userEntity) {
            if (userEntity == null) return null;
            
            return UserSummary.builder()
                    .userId(userEntity.getUserId())
                    .email(userEntity.getEmail())
                    .phone(userEntity.getPhone())
                    .nickname(userEntity.getNickname())
                    .profileImage(userEntity.getProfileImage())
                    .ratingAvg(userEntity.getRatingAvg())
                    .age(userEntity.getAge())
                    .role(userEntity.getRole())
                    .driver(userEntity.getDriver()) // Driver 정보가 필요하다면 추가
                    .build();
        }
    }
}