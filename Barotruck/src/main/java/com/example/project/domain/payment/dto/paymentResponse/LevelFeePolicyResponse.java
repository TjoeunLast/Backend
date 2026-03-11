package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LevelFeePolicyResponse(
        Long requestedLevel,
        Long appliedLevel,
        String side,
        BigDecimal rate,
        BigDecimal firstPaymentPromoRate,
        BigDecimal shipperRate,
        BigDecimal driverRate,
        BigDecimal shipperFirstPaymentPromoRate,
        BigDecimal driverFirstTransportPromoRate,
        BigDecimal tossRate,
        BigDecimal minFee,
        LocalDateTime updatedAt
) {
}
