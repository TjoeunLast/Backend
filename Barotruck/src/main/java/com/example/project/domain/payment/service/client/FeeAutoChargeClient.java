package com.example.project.domain.payment.service.client;

import java.math.BigDecimal;

public interface FeeAutoChargeClient {
    ChargeResult charge(Long invoiceId, Long shipperUserId, BigDecimal totalFee);

    record ChargeResult(
            boolean success,
            String failCode,
            String failReason,
            String orderId,
            String paymentKey,
            String transactionId,
            String rawPayload
    ) {}
}

