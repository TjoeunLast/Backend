package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentTiming;
import lombok.Getter;

@Getter
public class ExternalPayRequest {
    private PaymentMethod method;
    private PaymentTiming paymentTiming;
}
