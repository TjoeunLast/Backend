package com.example.project.domain.payment.dto.paymentRequest;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TossConfirmRequest {
    private String paymentKey;
    private String pgOrderId;
    private BigDecimal amount;
}
