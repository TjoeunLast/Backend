package com.example.project.domain.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "TRANSPORT_PAYMENTS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_TRANSPORT_PAYMENTS_ORDER", columnNames = {"ORDER_ID"})
        },
        indexes = {
                @Index(name = "IDX_TRANSPORT_PAYMENTS_ORDER", columnList = "ORDER_ID"),
                @Index(name = "IDX_TRANSPORT_PAYMENTS_SHIPPER", columnList = "SHIPPER_USER_ID"),
                @Index(name = "IDX_TRANSPORT_PAYMENTS_DRIVER", columnList = "DRIVER_USER_ID")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransportPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    @Column(name = "DRIVER_USER_ID", nullable = false)
    private Long driverUserId;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "FEE_RATE_SNAPSHOT", nullable = false, precision = 6, scale = 4)
    private BigDecimal feeRateSnapshot;

    @Column(name = "FEE_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmountSnapshot;

    @Column(name = "NET_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmountSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "METHOD", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private TransportPaymentStatus status;

    @Column(name = "PG_TID", length = 100)
    private String pgTid;

    @Column(name = "PROOF_URL", length = 500)
    private String proofUrl;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public static TransportPayment ready(
            Long orderId,
            Long shipperUserId,
            Long driverUserId,
            BigDecimal amount,
            BigDecimal feeRateSnapshot,
            BigDecimal feeAmountSnapshot,
            BigDecimal netAmountSnapshot,
            PaymentMethod method
    ) {
        return TransportPayment.builder()
                .orderId(orderId)
                .shipperUserId(shipperUserId)
                .driverUserId(driverUserId)
                .amount(amount)
                .feeRateSnapshot(feeRateSnapshot)
                .feeAmountSnapshot(feeAmountSnapshot)
                .netAmountSnapshot(netAmountSnapshot)
                .method(method)
                .status(TransportPaymentStatus.READY)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markPaid(String proofUrl, LocalDateTime paidAt) {
        this.status = TransportPaymentStatus.PAID;
        this.proofUrl = proofUrl;
        this.paidAt = (paidAt != null) ? paidAt : LocalDateTime.now();
    }

    public void confirm(LocalDateTime confirmedAt) {
        this.status = TransportPaymentStatus.CONFIRMED;
        this.confirmedAt = (confirmedAt != null) ? confirmedAt : LocalDateTime.now();
    }

    public void dispute() {
        this.status = TransportPaymentStatus.DISPUTED;
    }

    public void setPgTid(String pgTid) {
        this.pgTid = pgTid;
    }
}