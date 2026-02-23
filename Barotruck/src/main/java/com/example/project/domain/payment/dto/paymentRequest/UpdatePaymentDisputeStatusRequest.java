package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.PaymentDisputeStatus;
import lombok.Getter;

@Getter
public class UpdatePaymentDisputeStatusRequest {
    private PaymentDisputeStatus status;
    private String adminMemo;
}
