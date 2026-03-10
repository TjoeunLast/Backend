package com.example.project.domain.payment.dto.paymentRequest;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CancelTossPaymentRequest {
    private String cancelReason;
    private BigDecimal cancelAmount;
}
