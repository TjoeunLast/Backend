package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason;
import lombok.Getter;

@Getter
public class CreatePaymentDisputeRequest {
    private Long requesterUserId;
    private PaymentDisputeReason reasonCode;
    private String description;
    private String attachmentUrl;
}

