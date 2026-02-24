package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentMethod;
import lombok.Getter;

@Getter
public class TossPrepareRequest {
    private PaymentMethod method;
    private PayChannel payChannel;
    private String successUrl;
    private String failUrl;
}

