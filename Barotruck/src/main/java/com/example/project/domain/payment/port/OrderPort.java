package com.example.project.domain.payment.port;

import java.math.BigDecimal;

public interface OrderPort {

    OrderSnapshot getRequiredSnapshot(Long orderId);

    void setOrderPaid(Long orderId);
    void setOrderConfirmed(Long orderId);
    void setOrderDisputed(Long orderId);

    record OrderSnapshot(
            Long orderId,
            Long shipperUserId,
            Long driverUserId,
            BigDecimal amount,
            String status
    ) {}
}
