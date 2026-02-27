package com.example.project.domain.settlement.dto;

import com.example.project.domain.settlement.domain.Settlement;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SettlementResponse(
        Long settlementId,
        Long orderId,
        Long shipperUserId,
        Long driverUserId,
        Long levelDiscount,
        Long couponDiscount,
        Long totalPrice,
        Long feeRate,
        String status,
        LocalDateTime feeDate,
        LocalDateTime feeCompleteDate
) {
    public static SettlementResponse from(Settlement settlement) {
        return SettlementResponse.builder()
                .settlementId(settlement.getId())
                .orderId(settlement.getOrder() != null ? settlement.getOrder().getOrderId() : null)
                .shipperUserId(settlement.getUser() != null ? settlement.getUser().getUserId() : null)
                .driverUserId(settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null)
                .levelDiscount(settlement.getLevelDiscount())
                .couponDiscount(settlement.getCouponDiscount())
                .totalPrice(settlement.getTotalPrice())
                .feeRate(settlement.getFeeRate())
                .status(settlement.getStatus())
                .feeDate(settlement.getFeeDate())
                .feeCompleteDate(settlement.getFeeCompleteDate())
                .build();
    }
}
