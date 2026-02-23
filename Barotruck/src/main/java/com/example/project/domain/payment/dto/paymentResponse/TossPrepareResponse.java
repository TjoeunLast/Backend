package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.paymentEnum.PaymentProvider;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TossPrepareResponse {
    private PaymentProvider provider;
    private String pgOrderId;
    private BigDecimal amount;
    private String successUrl;
    private String failUrl;
    private LocalDateTime expiresAt;
}

