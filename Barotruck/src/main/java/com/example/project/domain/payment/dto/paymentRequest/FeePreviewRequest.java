package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FeePreviewRequest {
    private Long orderId;
    private Long shipperUserId;
    private Long driverUserId;
    private BigDecimal baseAmount;
    private PaymentProvider paymentProvider;
    private PaymentMethod paymentMethod;
    private PayChannel payChannel;
}
