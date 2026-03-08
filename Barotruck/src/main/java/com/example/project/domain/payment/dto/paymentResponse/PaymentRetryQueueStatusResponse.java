package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentRetryQueueStatusResponse(
        GatewayTxStatus status,
        long candidateCount,
        LocalDateTime firstTargetAt,
        Integer maxRetryAttempts
) {
}
