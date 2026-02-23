package com.example.project.domain.payment.service.client;

import java.math.BigDecimal;

public interface DriverPayoutGatewayClient {

    PayoutResult payout(Long orderId, Long driverUserId, BigDecimal netAmount, Long batchId, Long itemId);

    record PayoutResult(
            boolean success,
            String payoutRef,
            String failReason
    ) {}
}

