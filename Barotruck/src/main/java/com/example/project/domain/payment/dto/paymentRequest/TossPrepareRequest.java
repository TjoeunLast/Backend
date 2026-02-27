package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import lombok.Getter;

@Getter
public class TossPrepareRequest {
    private PaymentMethod method;
    private PayChannel payChannel;
    private String orderName;
}

