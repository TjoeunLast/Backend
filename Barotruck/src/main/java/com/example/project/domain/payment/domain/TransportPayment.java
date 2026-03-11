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

    // 주문 원금(base amount) 스냅샷
    @Column(name = "BASE_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal baseAmountSnapshot;

    // 수수료율 스냅샷
    @Column(name = "FEE_RATE_SNAPSHOT", nullable = false, precision = 6, scale = 4)
    private BigDecimal feeRateSnapshot;

    // 수수료 금액 스냅샷
    @Column(name = "FEE_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmountSnapshot;

    // 차주 실수령 금액 스냅샷
    @Column(name = "NET_AMOUNT_SNAPSHOT", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmountSnapshot;

    @Column(name = "SHIPPER_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal shipperFeeRateSnapshot;

    @Column(name = "SHIPPER_FEE_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal shipperFeeAmountSnapshot;

    @Column(name = "SHIPPER_PROMO_APPLIED")
    private Boolean shipperPromoApplied;

    @Column(name = "SHIPPER_CHARGE_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal shipperChargeAmountSnapshot;

    @Column(name = "DRIVER_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal driverFeeRateSnapshot;

    @Column(name = "DRIVER_FEE_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal driverFeeAmountSnapshot;

    @Column(name = "DRIVER_PROMO_APPLIED")
    private Boolean driverPromoApplied;

    @Column(name = "DRIVER_PAYOUT_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal driverPayoutAmountSnapshot;

    @Column(name = "TOSS_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal tossFeeRateSnapshot;

    @Column(name = "TOSS_FEE_AMOUNT_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal tossFeeAmountSnapshot;

    @Column(name = "PLATFORM_GROSS_REVENUE_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal platformGrossRevenueSnapshot;

    @Column(name = "PLATFORM_NET_REVENUE_SNAPSHOT", precision = 18, scale = 2)
    private BigDecimal platformNetRevenueSnapshot;

    @Column(name = "FEE_POLICY_ID_SNAPSHOT")
    private Long feePolicyIdSnapshot;

    @Column(name = "FEE_POLICY_APPLIED_AT_SNAPSHOT")
    private LocalDateTime feePolicyAppliedAtSnapshot;

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

    @Column(name = "FIRST_PAYMENT_PROMO_APPLIED", nullable = false, columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private boolean firstPaymentPromoApplied;

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
                .baseAmountSnapshot(netAmountSnapshot)
                .feeRateSnapshot(feeRateSnapshot)
                .feeAmountSnapshot(feeAmountSnapshot)
                .netAmountSnapshot(netAmountSnapshot)
                .shipperFeeRateSnapshot(feeRateSnapshot)
                .shipperFeeAmountSnapshot(feeAmountSnapshot)
                .shipperPromoApplied(false)
                .shipperChargeAmountSnapshot(amount)
                .driverFeeRateSnapshot(BigDecimal.ZERO.setScale(4))
                .driverFeeAmountSnapshot(BigDecimal.ZERO.setScale(2))
                .driverPromoApplied(false)
                .driverPayoutAmountSnapshot(netAmountSnapshot)
                .tossFeeRateSnapshot(BigDecimal.ZERO.setScale(4))
                .tossFeeAmountSnapshot(BigDecimal.ZERO.setScale(2))
                .platformGrossRevenueSnapshot(feeAmountSnapshot)
                .platformNetRevenueSnapshot(feeAmountSnapshot)
                .firstPaymentPromoApplied(false)
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
        this.confirmedAt = null;
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

    public void resetToReady() {
        this.status = TransportPaymentStatus.READY;
        this.proofUrl = null;
        this.paidAt = null;
        this.confirmedAt = null;
        this.pgTid = null;
    }

    public void cancel() {
        this.status = TransportPaymentStatus.CANCELLED;
        this.proofUrl = null;
        this.paidAt = null;
        this.confirmedAt = null;
        this.pgTid = null;
    }

    // PG 거래 식별자 반영
    public void setPgTid(String pgTid) {
        this.pgTid = pgTid;
    }

    public void applyMethod(PaymentMethod method) {
        if (method == null) {
            return;
        }
        this.method = method;
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
            this.shipperChargeAmountSnapshot = amount;
        }
        if (netAmountSnapshot != null) {
            this.baseAmountSnapshot = netAmountSnapshot;
            this.netAmountSnapshot = netAmountSnapshot;
            this.driverPayoutAmountSnapshot = netAmountSnapshot;
        }
        if (feeRateSnapshot != null) {
            this.feeRateSnapshot = feeRateSnapshot;
            this.shipperFeeRateSnapshot = feeRateSnapshot;
        }
        if (feeAmountSnapshot != null) {
            this.feeAmountSnapshot = feeAmountSnapshot;
            this.shipperFeeAmountSnapshot = feeAmountSnapshot;
            this.platformGrossRevenueSnapshot = feeAmountSnapshot;
            if (this.tossFeeAmountSnapshot == null) {
                this.platformNetRevenueSnapshot = feeAmountSnapshot;
            } else {
                this.platformNetRevenueSnapshot = feeAmountSnapshot.subtract(this.tossFeeAmountSnapshot);
            }
        }
    }

    public void applyPricingSnapshot(TransportPaymentPricingSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.actualPaidAmount() != null) {
            this.amount = snapshot.actualPaidAmount();
        }
        if (snapshot.baseAmount() != null) {
            this.baseAmountSnapshot = snapshot.baseAmount();
        }
        if (snapshot.shipperFeeRate() != null) {
            this.feeRateSnapshot = snapshot.shipperFeeRate();
            this.shipperFeeRateSnapshot = snapshot.shipperFeeRate();
        }
        if (snapshot.shipperFeeAmount() != null) {
            this.feeAmountSnapshot = snapshot.shipperFeeAmount();
            this.shipperFeeAmountSnapshot = snapshot.shipperFeeAmount();
        }
        if (snapshot.driverPayoutAmount() != null) {
            this.netAmountSnapshot = snapshot.driverPayoutAmount();
            this.driverPayoutAmountSnapshot = snapshot.driverPayoutAmount();
        }
        this.shipperPromoApplied = snapshot.shipperPromoApplied();
        this.firstPaymentPromoApplied = snapshot.shipperPromoApplied();
        if (snapshot.shipperChargeAmount() != null) {
            this.shipperChargeAmountSnapshot = snapshot.shipperChargeAmount();
        }
        if (snapshot.driverFeeRate() != null) {
            this.driverFeeRateSnapshot = snapshot.driverFeeRate();
        }
        if (snapshot.driverFeeAmount() != null) {
            this.driverFeeAmountSnapshot = snapshot.driverFeeAmount();
        }
        this.driverPromoApplied = snapshot.driverPromoApplied();
        if (snapshot.tossFeeRate() != null) {
            this.tossFeeRateSnapshot = snapshot.tossFeeRate();
        }
        if (snapshot.tossFeeAmount() != null) {
            this.tossFeeAmountSnapshot = snapshot.tossFeeAmount();
        }
        if (snapshot.platformGrossRevenue() != null) {
            this.platformGrossRevenueSnapshot = snapshot.platformGrossRevenue();
        }
        if (snapshot.platformNetRevenue() != null) {
            this.platformNetRevenueSnapshot = snapshot.platformNetRevenue();
        }
        if (snapshot.feePolicyId() != null) {
            this.feePolicyIdSnapshot = snapshot.feePolicyId();
        }
        if (snapshot.feePolicyAppliedAt() != null) {
            this.feePolicyAppliedAtSnapshot = snapshot.feePolicyAppliedAt();
        }
    }

    public void applyFirstPaymentPromo(boolean applied) {
        this.firstPaymentPromoApplied = applied;
        this.shipperPromoApplied = applied;
    }
}
