package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
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
        PaymentTiming paymentTiming,
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
                .paymentTiming(p.getPaymentTiming())
                .status(p.getStatus())
                .pgTid(p.getPgTid())
                .proofUrl(p.getProofUrl())
                .paidAt(p.getPaidAt())
                .confirmedAt(p.getConfirmedAt())
                .build();
    }
}
