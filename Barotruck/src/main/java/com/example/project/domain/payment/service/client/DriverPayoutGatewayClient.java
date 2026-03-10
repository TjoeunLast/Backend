package com.example.project.domain.payment.service.client;

import java.math.BigDecimal;

public interface DriverPayoutGatewayClient {

    PayoutResult payout(Long orderId, Long driverUserId, BigDecimal netAmount, Long batchId, Long itemId);

    PayoutStatusResult getPayoutStatus(String payoutRef);

    record PayoutResult(
            boolean success,
            boolean completed,
            String payoutRef,
            String gatewayStatus,
            String failReason
    ) {}

    record PayoutStatusResult(
            boolean success,
            boolean completed,
            boolean failed,
            String gatewayStatus,
            String failReason
    ) {}
}

