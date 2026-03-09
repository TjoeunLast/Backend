package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
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
        String payoutRef
) {
    public static DriverPayoutItemStatusResponse from(DriverPayoutItem item) {
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
                .build();
    }
}
