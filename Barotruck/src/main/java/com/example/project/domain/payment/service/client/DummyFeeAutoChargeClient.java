package com.example.project.domain.payment.service.client;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "payment.fee-auto-charge.mock-enabled", havingValue = "true")
public class DummyFeeAutoChargeClient implements FeeAutoChargeClient {

    @Override
    public ChargeResult charge(Long invoiceId, Long shipperUserId, BigDecimal totalFee) {
        return new ChargeResult(
                true,
                null,
                null,
                "DUMMY-AUTO-CHARGE-" + invoiceId,
                "DUMMY-PAYMENT-KEY",
                "DUMMY-TX",
                "{}"
        );
    }
}

