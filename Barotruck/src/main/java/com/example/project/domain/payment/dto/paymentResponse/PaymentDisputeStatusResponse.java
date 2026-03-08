package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentDisputeStatusResponse(
        Long disputeId,
        Long orderId,
        Long paymentId,
        PaymentDisputeStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
    public static PaymentDisputeStatusResponse from(PaymentDispute dispute) {
        return PaymentDisputeStatusResponse.builder()
                .disputeId(dispute.getDisputeId())
                .orderId(dispute.getOrderId())
                .paymentId(dispute.getPaymentId())
                .status(dispute.getStatus())
                .requestedAt(dispute.getRequestedAt())
                .processedAt(dispute.getProcessedAt())
                .build();
    }
}
