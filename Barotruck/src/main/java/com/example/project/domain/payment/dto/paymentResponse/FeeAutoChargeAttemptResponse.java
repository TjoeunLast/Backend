package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeAutoChargeAttempt;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeAutoChargeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FeeAutoChargeAttemptResponse(
        Long attemptId,
        Long invoiceId,
        Long shipperUserId,
        Long agreementId,
        PaymentProvider provider,
        FeeAutoChargeStatus status,
        String orderId,
        String paymentKey,
        String transactionId,
        BigDecimal amount,
        String failCode,
        String failReason,
        LocalDateTime attemptedAt,
        String rawPayloadSummary
) {
    public static FeeAutoChargeAttemptResponse from(FeeAutoChargeAttempt attempt) {
        return FeeAutoChargeAttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .invoiceId(attempt.getInvoiceId())
                .shipperUserId(attempt.getShipperUserId())
                .agreementId(attempt.getAgreementId())
                .provider(attempt.getProvider())
                .status(attempt.getStatus())
                .orderId(attempt.getOrderId())
                .paymentKey(attempt.getPaymentKey())
                .transactionId(attempt.getTransactionId())
                .amount(attempt.getAmount())
                .failCode(attempt.getFailCode())
                .failReason(attempt.getFailReason())
                .attemptedAt(attempt.getAttemptedAt())
                .rawPayloadSummary(summarizePayload(attempt.getRawPayload()))
                .build();
    }

    private static String summarizePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return null;
        }
        String normalized = rawPayload.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 197) + "...";
    }
}
