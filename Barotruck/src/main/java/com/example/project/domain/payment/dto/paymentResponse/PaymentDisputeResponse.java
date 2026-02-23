package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.paymentEnum.PaymentDisputeReason;
import com.example.project.domain.payment.domain.paymentEnum.PaymentDisputeStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentDisputeResponse(
        Long disputeId,
        Long orderId,
        Long paymentId,
        Long requesterUserId,
        Long createdByUserId,
        PaymentDisputeReason reasonCode,
        String description,
        String attachmentUrl,
        PaymentDisputeStatus status,
        String adminMemo,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
    public static PaymentDisputeResponse from(PaymentDispute dispute) {
        return PaymentDisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .orderId(dispute.getOrderId())
                .paymentId(dispute.getPaymentId())
                .requesterUserId(dispute.getRequesterUserId())
                .createdByUserId(dispute.getCreatedByUserId())
                .reasonCode(dispute.getReasonCode())
                .description(dispute.getDescription())
                .attachmentUrl(dispute.getAttachmentUrl())
                .status(dispute.getStatus())
                .adminMemo(dispute.getAdminMemo())
                .requestedAt(dispute.getRequestedAt())
                .processedAt(dispute.getProcessedAt())
                .build();
    }
}

