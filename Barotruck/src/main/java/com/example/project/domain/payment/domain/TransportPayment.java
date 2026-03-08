package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
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
                @Index(name = "IDX_TRANSPORT_PAYMENTS_SHIPPER", columnList = "SHIPPER_USER_ID"),
                @Index(name = "IDX_TRANSPORT_PAYMENTS_DRIVER", columnList = "DRIVER_USER_ID")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransportPayment {

    // 결제 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    // 주문 ID(주문당 1건 결제)
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    // 화주 사용자 ID
    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    // 차주 사용자 ID
    @Column(name = "DRIVER_USER_ID", nullable = false)
    private Long driverUserId;

    // 화주 청구 금액 스냅샷
    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // 수수료율 스냅샷
    @Column(name = "FEE_RATE_SNAPSHOT", nullable = false, precision = 6, scale = 4)
    private BigDecimal feeRateSnapshot;

    // 수수료 금액 스냅샷
    @Column(name = "FEE_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmountSnapshot;

    // 차주 실수령 금액 스냅샷
    @Column(name = "NET_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmountSnapshot;

    // 결제 수단
    @Enumerated(EnumType.STRING)
    @Column(name = "METHOD", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TIMING", length = 20)
    private PaymentTiming paymentTiming;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 40)
    private TransportPaymentStatus status;

    // PG 거래 ID
    @Column(name = "PG_TID", length = 100)
    private String pgTid;

    // 결제 증빙 URL(영수증/거래내역 등)
    @Column(name = "PROOF_URL", length = 500)
    private String proofUrl;

    // 결제 완료 시각
    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    // 차주 확정 시각
    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt;

    // 결제 레코드 생성 시각
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // READY 상태의 결제 스냅샷 생성
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
        return ready(
                orderId,
                shipperUserId,
                driverUserId,
                amount,
                feeRateSnapshot,
                feeAmountSnapshot,
                netAmountSnapshot,
                method,
                PaymentTiming.PREPAID
        );
    }

    public static TransportPayment ready(
            Long orderId,
            Long shipperUserId,
            Long driverUserId,
            BigDecimal amount,
            BigDecimal feeRateSnapshot,
            BigDecimal feeAmountSnapshot,
            BigDecimal netAmountSnapshot,
            PaymentMethod method,
            PaymentTiming paymentTiming
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
                .paymentTiming(paymentTiming)
                .status(TransportPaymentStatus.READY)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 결제 완료 처리
    public void markPaid(String proofUrl, LocalDateTime paidAt) {
        this.status = TransportPaymentStatus.PAID;
        this.proofUrl = proofUrl;
        this.paidAt = (paidAt != null) ? paidAt : LocalDateTime.now();
    }

    // 차주 결제 확인 처리
    public void confirm(LocalDateTime confirmedAt) {
        this.status = TransportPaymentStatus.CONFIRMED;
        this.confirmedAt = (confirmedAt != null) ? confirmedAt : LocalDateTime.now();
    }

    // 결제 이의 상태로 전환
    public void dispute() {
        this.status = TransportPaymentStatus.DISPUTED;
    }

    // 관리자 처리 등 외부 상태 반영
    public void updateStatus(TransportPaymentStatus status) {
        this.status = status;
    }

    // PG 거래 식별자 반영
    public void setPgTid(String pgTid) {
        this.pgTid = pgTid;
    }

    public void applyPaymentTiming(PaymentTiming paymentTiming) {
        if (paymentTiming == null) {
            return;
        }
        this.paymentTiming = paymentTiming;
    }

    public void applyPricingSnapshots(
            BigDecimal amount,
            BigDecimal feeRateSnapshot,
            BigDecimal feeAmountSnapshot,
            BigDecimal netAmountSnapshot
    ) {
        if (amount != null) {
            this.amount = amount;
        }
        if (feeRateSnapshot != null) {
            this.feeRateSnapshot = feeRateSnapshot;
        }
        if (feeAmountSnapshot != null) {
            this.feeAmountSnapshot = feeAmountSnapshot;
        }
        if (netAmountSnapshot != null) {
            this.netAmountSnapshot = netAmountSnapshot;
        }
    }
}
