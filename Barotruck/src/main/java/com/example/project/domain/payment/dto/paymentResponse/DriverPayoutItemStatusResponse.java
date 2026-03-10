package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.member.domain.Driver;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DriverPayoutItemStatusResponse(
        Long itemId,
        Long orderId,
        Long batchId,
        Long driverUserId,
        PayoutStatus status,
        Integer retryCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String failureReason,
        String payoutRef,
        String sellerId,
        String sellerRef,
        String sellerStatus,
        Long lastWebhookId,
        String lastWebhookExternalEventId,
        String lastWebhookEventType,
        String lastWebhookProcessResult,
        String webhookStatus,
        LocalDateTime lastWebhookReceivedAt,
        LocalDateTime lastWebhookProcessedAt,
        Boolean webhookMatchesPayoutStatus
) {
    public static DriverPayoutItemStatusResponse from(DriverPayoutItem item) {
        return from(item, null, null, null, null);
    }

    public static DriverPayoutItemStatusResponse from(
            DriverPayoutItem item,
            Driver driver,
            PaymentGatewayWebhookEvent latestWebhookEvent,
            String webhookStatus,
            Boolean webhookMatchesPayoutStatus
    ) {
        return DriverPayoutItemStatusResponse.builder()
                .itemId(item.getItemId())
                .orderId(item.getOrderId())
                .batchId(item.getBatch() == null ? null : item.getBatch().getBatchId())
                .driverUserId(item.getDriverUserId())
                .status(item.getStatus())
                .retryCount(item.getRetryCount())
                .requestedAt(item.getRequestedAt())
                .completedAt(item.getCompletedAt())
                .failureReason(item.getFailureReason())
                .payoutRef(item.getPayoutRef())
                .sellerId(driver == null ? null : driver.getTossPayoutSellerId())
                .sellerRef(driver == null ? null : driver.getTossPayoutSellerRef())
                .sellerStatus(driver == null ? null : driver.getTossPayoutSellerStatus())
                .lastWebhookId(latestWebhookEvent == null ? null : latestWebhookEvent.getWebhookId())
                .lastWebhookExternalEventId(
                        latestWebhookEvent == null ? null : latestWebhookEvent.getExternalEventId()
                )
                .lastWebhookEventType(latestWebhookEvent == null ? null : latestWebhookEvent.getEventType())
                .lastWebhookProcessResult(
                        latestWebhookEvent == null ? null : latestWebhookEvent.getProcessResult()
                )
                .webhookStatus(webhookStatus)
                .lastWebhookReceivedAt(
                        latestWebhookEvent == null ? null : latestWebhookEvent.getReceivedAt()
                )
                .lastWebhookProcessedAt(
                        latestWebhookEvent == null ? null : latestWebhookEvent.getProcessedAt()
                )
                .webhookMatchesPayoutStatus(webhookMatchesPayoutStatus)
                .build();
    }
}
