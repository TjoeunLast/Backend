package com.example.project.domain.payment.service.client;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DummyFeeAutoChargeClient implements FeeAutoChargeClient {

    @Override
    public ChargeResult charge(Long shipperUserId, BigDecimal totalFee) {
        return new ChargeResult(true, null);
    }
}

