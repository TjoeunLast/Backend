package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.*;
import com.example.project.domain.payment.domain.TransportPayment;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransportPaymentResponse(
        Long paymentId,
        Long orderId,
        Long shipperUserId,
        Long driverUserId,
        BigDecimal amount,
        BigDecimal feeRateSnapshot,
        BigDecimal feeAmountSnapshot,
        BigDecimal netAmountSnapshot,
        PaymentMethod method,
        TransportPaymentStatus status,
        String pgTid,
        String proofUrl,
        LocalDateTime paidAt,
        LocalDateTime confirmedAt
) {
    public static TransportPaymentResponse from(TransportPayment p) {
        return TransportPaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .orderId(p.getOrderId())
                .shipperUserId(p.getShipperUserId())
                .driverUserId(p.getDriverUserId())
                .amount(p.getAmount())
                .feeRateSnapshot(p.getFeeRateSnapshot())
                .feeAmountSnapshot(p.getFeeAmountSnapshot())
                .netAmountSnapshot(p.getNetAmountSnapshot())
                .method(p.getMethod())
                .status(p.getStatus())
                .pgTid(p.getPgTid())
                .proofUrl(p.getProofUrl())
                .paidAt(p.getPaidAt())
                .confirmedAt(p.getConfirmedAt())
                .build();
    }
}
