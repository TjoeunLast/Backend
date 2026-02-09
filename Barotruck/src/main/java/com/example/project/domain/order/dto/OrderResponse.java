package com.example.project.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.global.image.ImageInfo;
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
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updated;

    // 상차지
    private String startAddr;
    private String startPlace;
    private String startType;
    private String startSchedule;
    private String puProvince;

    // 하차지
    private String endAddr;
    private String endPlace;
    private String endType;
    private String endSchedule;
    private String doProvince;

    // 화물/작업
    private String cargoContent;
    private String loadMethod;
    private String workType;
    private BigDecimal tonnage;
    private String reqCarType;
    private String reqTonnage;
    private String driveMode;
    private Long loadWeight;

    // 요금
    private Long basePrice;
    private Long laborFee;
    private Long packagingPrice;
    private Long insuranceFee;
    private String payMethod;
    
    // 시스템 지표
    private Long distance;
    private Long duration;

    private UserSummary user;
    private CancellationSummary cancellation;

    public static OrderResponse from(Order order) {
        OrderSnapshot s = order.getSnapshot();
        if (s == null) return null;

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updated(order.getUpdated())
                .distance(order.getDistance())
                .duration(order.getDuration())
                // Snapshot 데이터 매핑
                .startAddr(s.getStartAddr())
                .startPlace(s.getStartPlace())
                .startType(s.getStartType())
                .startSchedule(s.getStartSchedule())
                .puProvince(s.getPuProvince())
                .endAddr(s.getEndAddr())
                .endPlace(s.getEndPlace())
                .endType(s.getEndType())
                .endSchedule(s.getEndSchedule())
                .doProvince(s.getDoProvince())
                .cargoContent(s.getCargoContent())
                .loadMethod(s.getLoadMethod())
                .workType(s.getWorkType())
                .tonnage(s.getTonnage())
                .reqCarType(s.getReqCarType())
                .reqTonnage(s.getReqTonnage())
                .driveMode(s.getDriveMode())
                .loadWeight(s.getLoadWeight())
                .basePrice(s.getBasePrice())
                .laborFee(s.getLaborFee())
                .packagingPrice(s.getPackagingPrice())
                .insuranceFee(s.getInsuranceFee())
                .payMethod(s.getPayMethod())
                // 요약 정보
                .user(UserSummary.from(order.getUser()))
                .cancellation(CancellationSummary.from(order.getCancellationInfo()))
                .build();
    }

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
        private Long level;
        private Role role;

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
                    .level(userEntity.getUser_level())
                    .role(userEntity.getRole())
                    .build();
        }
    }

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