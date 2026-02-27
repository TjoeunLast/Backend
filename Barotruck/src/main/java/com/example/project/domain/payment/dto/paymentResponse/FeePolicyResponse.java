package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FeePolicyResponse(
        BigDecimal level0Rate,
        BigDecimal level1Rate,
        BigDecimal level2Rate,
        BigDecimal level3PlusRate,
        BigDecimal firstPaymentPromoRate,
        BigDecimal minFee,
        LocalDateTime updatedAt
) {
}

