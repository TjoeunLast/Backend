package com.example.project.domain.payment.service;

import com.example.project.domain.payment.domain.PaymentMethod;

public interface ExternalPaymentClient {
    ExternalPayResult pay(String merchantOrderId, long amount, PaymentMethod method);

    record ExternalPayResult(
            boolean success,
            String transactionId,
            String failReason
    ) {}
}
