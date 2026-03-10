package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record TossPaymentLookupResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        String easyPayProvider,
        BigDecimal totalAmount,
        BigDecimal suppliedAmount,
        BigDecimal vat,
        LocalDateTime approvedAt,
        LocalDateTime lastTransactionAt,
        List<CancelHistory> cancels,
        String rawPayload
) {
    @Builder
    public record CancelHistory(
            BigDecimal cancelAmount,
            String cancelReason,
            LocalDateTime canceledAt,
            String transactionKey,
            String cancelStatus
    ) {
    }
}
