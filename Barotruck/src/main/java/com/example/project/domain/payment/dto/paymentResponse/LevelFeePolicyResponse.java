package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LevelFeePolicyResponse(
        Long requestedLevel,
        Long appliedLevel,
        BigDecimal rate,
        BigDecimal firstPaymentPromoRate,
        BigDecimal minFee,
        LocalDateTime updatedAt
) {
}
