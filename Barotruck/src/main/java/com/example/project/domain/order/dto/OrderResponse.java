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
    // 1. 주문 기본 정보 및 상태
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updated;

    // 2. 상차지 상세 정보 (Request 필드 반영)
    private String startAddr;
    private String startPlace;      // 추가: 장소명
    private String startType;
    private String startSchedule;
    private String puProvince;      // 추가: 시/도
    private Long startNeighborhoodId;

    // 3. 하차지 상세 정보 (Request 필드 반영)
    private String endAddr;
    private String endPlace;        // 추가: 장소명
    private String endType;
    private String endSchedule;
    private String doProvince;      // 추가: 시/도
    private Long endNeighborhoodId;

    // 4. 화물 및 작업 세부 정보
    private String cargoContent;
    private String loadMethod;
    private String workType;
    private BigDecimal tonnage;
    private String reqCarType;
    private String reqTonnage;
    private String driveMode;
    private Long loadWeight;

    // 5. 요금 및 정산 정보 (Embedded 필드 및 추가비용 반영)
    private Long basePrice;
    private Long laborFee;
    private Long packagingPrice;   // 추가: 포장비
    private Long insuranceFee;     // 추가: 보험료
    private String payMethod;
    private Long feeRate;
    private Long totalPrice;

    // 6. 시스템 계산 지표 (지도 API 결과)
    private Long distance;         // 추가: 거리
    private Long duration;         // 추가: 소요시간

    // 9. 화주(사용자) 정보
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
        private Long level; // 추가: 유저 등급
        private Role role;
        // Driver 정보는 필요한 최소 정보만 노출하거나 별도 DTO 권장

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
                    .level(userEntity.getUser_level()) // 유저 등급 반영
                    .role(userEntity.getRole())
                    .build();
        }
    }
    
    
 // 7. 취소 정보 (추가)
    private CancellationSummary cancellation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationSummary {
        private String cancelReason;
        private LocalDateTime cancelledAt;
        private String cancelledBy;

        public static CancellationSummary from(com.example.project.domain.order.domain.CancellationInfo info) {
            if (info == null) return null;
            return CancellationSummary.builder()
                    .cancelReason(info.getCancelReason())
                    .cancelledAt(info.getCancelledAt())
                    .cancelledBy(info.getCancelledBy())
                    .build();
        }
    }
}