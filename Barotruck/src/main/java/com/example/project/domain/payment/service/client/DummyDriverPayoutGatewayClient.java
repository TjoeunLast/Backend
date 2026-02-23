package com.example.project.domain.payment.service.client;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DummyDriverPayoutGatewayClient implements DriverPayoutGatewayClient {

    @Override
    public PayoutResult payout(Long orderId, Long driverUserId, BigDecimal netAmount, Long batchId, Long itemId) {
        return new PayoutResult(true, "PAYOUT-" + UUID.randomUUID(), null);
    }
}

