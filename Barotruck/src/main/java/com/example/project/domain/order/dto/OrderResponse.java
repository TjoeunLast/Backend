package com.example.project.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.global.image.ImageInfo;
import com.example.project.security.user.Role;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updated;

    // 정산 상태 필드
    private String settlementStatus;

    // 상차지
    private String startAddr;
    private String startPlace;
    private String startType;
    private String startSchedule;
    private String puProvince;

    private Long driverNo;
    
    // 상차지 좌표
    private BigDecimal startLat;
    private BigDecimal startLng;

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
    private List<Long> driverList;
    
    
    // 요금
    private Long basePrice;
    private Long laborFee;
    private Long packagingPrice;
    private Long insuranceFee;
    private String payMethod;

    private boolean instant; // 즉시배차 , 배정배차
    private String memo;
    private List<String> tag;

    // 오더의 출발/도착 지역 코드
    private Long startNbhId;
    private Long endNbhId;

    // 시스템 지표
    private BigDecimal distance;
    private BigDecimal duration;

    private UserSummary user;
    private CancellationSummary cancellation;
    private PaymentSummary paymentSummary;
    private ProofSummary proofSummary;
    private DisputeSummary disputeSummary;

    public static OrderResponse from(Order order) {
        OrderSnapshot s = order.getSnapshot();
        if (s == null)
            return null;

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())

                // 매필 로직 추가 정산
                .settlementStatus(order.getSettlement() != null ? order.getSettlement().getStatus() : "READY")
                .driverNo(order.getDriverNo())
                .createdAt(order.getCreatedAt())
                .updated(order.getUpdated())
                .distance(order.getDistance())
                .duration(order.getDuration())
                .driverList(order.getDriverList())
                
                // Snapshot 데이터 매핑
                .startLat(s.getStartLat())
                .startLng(s.getStartLng())
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
                .instant(s.isInstant())
                .memo(s.getMemo())
                .tag(s.getTag())
                .startNbhId(s.getStartNbhId())
                .endNbhId(s.getEndNbhId())
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
            if (userEntity == null)
                return null;
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
            if (info == null)
                return null;
            return CancellationSummary.builder()
                    .cancelReason(info.getCancelReason())
                    .cancelledAt(info.getCancelledAt())
                    .cancelledBy(info.getCancelledBy())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private Long paymentId;
        private BigDecimal chargedAmount;
        private BigDecimal receivedAmount;
        private BigDecimal feeAmount;
        private PaymentMethod method;
        private TransportPaymentStatus status;
        private LocalDateTime paidAt;
        private LocalDateTime confirmedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProofSummary {
        private Long proofId;
        private String receiptImageUrl;
        private String signatureImageUrl;
        private String recipientName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisputeSummary {
        private Long disputeId;
        private Long requesterUserId;
        private Long createdByUserId;
        private PaymentDisputeReason reasonCode;
        private String description;
        private String attachmentUrl;
        private PaymentDisputeStatus status;
        private String adminMemo;
        private LocalDateTime requestedAt;
        private LocalDateTime processedAt;
    }

}
