package com.example.project.domain.order.dto.orderResponse;

import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.security.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminOrderDetailResponse {

    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updated;
    private Long driverNo;

    private Long shipperUserId;
    private String shipperNickname;
    private Role shipperRole;

    private String startAddr;
    private String startPlace;
    private String startSchedule;
    private String puProvince;

    private String endAddr;
    private String endPlace;
    private String endSchedule;
    private String doProvince;

    private String cargoContent;
    private String loadMethod;
    private String workType;
    private BigDecimal tonnage;
    private String reqCarType;
    private String reqTonnage;
    private String driveMode;
    private Long loadWeight;

    private Long basePrice;
    private Long laborFee;
    private Long packagingPrice;
    private Long insuranceFee;
    private String payMethod;
    private boolean instant;
    private String memo;
    private List<String> tag;

    private Long distance;
    private Long duration;

    private CancellationSummary cancellation;

    public static AdminOrderDetailResponse from(Order order) {
        OrderSnapshot snapshot = order.getSnapshot();
        return AdminOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updated(order.getUpdated())
                .driverNo(order.getDriverNo())
                .shipperUserId(order.getUser() != null ? order.getUser().getUserId() : null)
                .shipperNickname(order.getUser() != null ? order.getUser().getNickname() : null)
                .shipperRole(order.getUser() != null ? order.getUser().getRole() : null)
                .startAddr(snapshot != null ? snapshot.getStartAddr() : null)
                .startPlace(snapshot != null ? snapshot.getStartPlace() : null)
                .startSchedule(snapshot != null ? snapshot.getStartSchedule() : null)
                .puProvince(snapshot != null ? snapshot.getPuProvince() : null)
                .endAddr(snapshot != null ? snapshot.getEndAddr() : null)
                .endPlace(snapshot != null ? snapshot.getEndPlace() : null)
                .endSchedule(snapshot != null ? snapshot.getEndSchedule() : null)
                .doProvince(snapshot != null ? snapshot.getDoProvince() : null)
                .cargoContent(snapshot != null ? snapshot.getCargoContent() : null)
                .loadMethod(snapshot != null ? snapshot.getLoadMethod() : null)
                .workType(snapshot != null ? snapshot.getWorkType() : null)
                .tonnage(snapshot != null ? snapshot.getTonnage() : null)
                .reqCarType(snapshot != null ? snapshot.getReqCarType() : null)
                .reqTonnage(snapshot != null ? snapshot.getReqTonnage() : null)
                .driveMode(snapshot != null ? snapshot.getDriveMode() : null)
                .loadWeight(snapshot != null ? snapshot.getLoadWeight() : null)
                .basePrice(snapshot != null ? snapshot.getBasePrice() : null)
                .laborFee(snapshot != null ? snapshot.getLaborFee() : null)
                .packagingPrice(snapshot != null ? snapshot.getPackagingPrice() : null)
                .insuranceFee(snapshot != null ? snapshot.getInsuranceFee() : null)
                .payMethod(snapshot != null ? snapshot.getPayMethod() : null)
                .instant(snapshot != null && snapshot.isInstant())
                .memo(snapshot != null ? snapshot.getMemo() : null)
                .tag(snapshot != null ? snapshot.getTag() : null)
                .distance(order.getDistance())
                .duration(order.getDuration())
                .cancellation(CancellationSummary.from(order.getCancellationInfo()))
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CancellationSummary {
        private String cancelReason;
        private LocalDateTime cancelledAt;
        private String cancelledBy;

        public static CancellationSummary from(CancellationInfo info) {
            if (info == null) {
                return null;
            }
            return CancellationSummary.builder()
                    .cancelReason(info.getCancelReason())
                    .cancelledAt(info.getCancelledAt())
                    .cancelledBy(info.getCancelledBy())
                    .build();
        }
    }
}
