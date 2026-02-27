package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TossPrepareResponse {
    private PaymentProvider provider;
    private String clientKey;
    private Long orderId;
    private String pgOrderId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PayChannel payChannel;
    private String orderName;
    private String successUrl;
    private String failUrl;
    private String confirmEndpoint;
    private LocalDateTime expiresAt;
}

