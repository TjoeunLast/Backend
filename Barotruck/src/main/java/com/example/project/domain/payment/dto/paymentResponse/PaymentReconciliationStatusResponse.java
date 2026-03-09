package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

@Builder
public record PaymentReconciliationStatusResponse(
        long confirmedGatewayCount,
        long matchedPaymentCount,
        long unresolvedMismatchCount
) {
}
