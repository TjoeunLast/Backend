package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FeePolicySideResponse(
        BigDecimal level0Rate,
        BigDecimal level1Rate,
        BigDecimal level2Rate,
        BigDecimal level3PlusRate
) {
}
