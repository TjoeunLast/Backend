package com.example.project.domain.payment.service.client;

import java.math.BigDecimal;

public interface FeeAutoChargeClient {
    ChargeResult charge(Long shipperUserId, BigDecimal totalFee);

    record ChargeResult(boolean success, String failReason) {}
}

