package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import lombok.Getter;

@Getter
public class ExternalPayRequest {
    private PaymentMethod method;
    private PaymentTiming paymentTiming;
}
