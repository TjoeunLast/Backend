package com.example.project.domain.payment.service.client;

import java.math.BigDecimal;

public interface TossPaymentClient {

    ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount);

    record ConfirmResult(
            boolean success,
            String transactionId,
            String failCode,
            String failMessage,
            String rawPayload
    ) {}
}

