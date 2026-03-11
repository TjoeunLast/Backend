package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FeePolicyResponse(
        Long policyConfigId,
        BigDecimal level0Rate,
        BigDecimal level1Rate,
        BigDecimal level2Rate,
        BigDecimal level3PlusRate,
        BigDecimal firstPaymentPromoRate,
        FeePolicySideResponse shipperSide,
        FeePolicySideResponse driverSide,
        BigDecimal shipperFirstPaymentPromoRate,
        BigDecimal driverFirstTransportPromoRate,
        BigDecimal tossRate,
        BigDecimal minFee,
        LocalDateTime updatedAt
) {
}
