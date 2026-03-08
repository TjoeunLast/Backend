package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record GatewayTransactionStatusResponse(
        Long txId,
        Long orderId,
        PaymentProvider provider,
        GatewayTxStatus status,
        BigDecimal amount,
        Integer retryCount,
        LocalDateTime expiresAt,
        LocalDateTime approvedAt,
        LocalDateTime nextRetryAt,
        String failCode,
        String failMessage
) {
    public static GatewayTransactionStatusResponse from(PaymentGatewayTransaction tx) {
        return GatewayTransactionStatusResponse.builder()
                .txId(tx.getTxId())
                .orderId(tx.getOrderId())
                .provider(tx.getProvider())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .retryCount(tx.getRetryCount())
                .expiresAt(tx.getExpiresAt())
                .approvedAt(tx.getApprovedAt())
                .nextRetryAt(tx.getNextRetryAt())
                .failCode(tx.getFailCode())
                .failMessage(tx.getFailMessage())
                .build();
    }
}
