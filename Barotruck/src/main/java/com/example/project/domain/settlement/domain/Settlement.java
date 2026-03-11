package com.example.project.domain.settlement.domain;


import com.example.project.domain.order.domain.Order;
import com.example.project.domain.payment.domain.TransportPaymentPricingSnapshot;
import com.example.project.member.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "SETTLEMENT")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SETTLEMENT_ID")
    private Long id;

    // 1. 연관된 주문 정보 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    // 2. 할인 정보 (이미지: level_discount, coupon_discount)
    @Column(name = "LEVEL_DISCOUNT")
    private Long levelDiscount; // 사용자 등급에 따른 할인액

    @Column(name = "COUPON_DISCOUNT")
    private Long couponDiscount; // 쿠폰 사용에 따른 할인액

    // 3. 최종 정산 금액 및 수수료 (이미지: TOTAL_PRICE, FEE_RATE)
    @Column(name = "TOTAL_PRICE")
    private Long totalPrice; // 할인 등이 모두 적용된 최종 결제 금액

    @Column(name = "FEE_RATE")
    private Long feeRate; // 플랫폼 수수료율 (예: 10)

    @Column(name = "BASE_AMOUNT_SNAPSHOT")
    private Long baseAmountSnapshot;

    @Column(name = "SHIPPER_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal shipperFeeRateSnapshot;

    @Column(name = "SHIPPER_FEE_AMOUNT_SNAPSHOT")
    private Long shipperFeeAmountSnapshot;

    @Column(name = "SHIPPER_PROMO_APPLIED")
    private Boolean shipperPromoApplied;

    @Column(name = "SHIPPER_CHARGE_AMOUNT_SNAPSHOT")
    private Long shipperChargeAmountSnapshot;

    @Column(name = "DRIVER_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal driverFeeRateSnapshot;

    @Column(name = "DRIVER_FEE_AMOUNT_SNAPSHOT")
    private Long driverFeeAmountSnapshot;

    @Column(name = "DRIVER_PROMO_APPLIED")
    private Boolean driverPromoApplied;

    @Column(name = "DRIVER_PAYOUT_AMOUNT_SNAPSHOT")
    private Long driverPayoutAmountSnapshot;

    @Column(name = "TOSS_FEE_RATE_SNAPSHOT", precision = 6, scale = 4)
    private BigDecimal tossFeeRateSnapshot;

    @Column(name = "TOSS_FEE_AMOUNT_SNAPSHOT")
    private Long tossFeeAmountSnapshot;

    @Column(name = "PLATFORM_GROSS_REVENUE_SNAPSHOT")
    private Long platformGrossRevenueSnapshot;

    @Column(name = "PLATFORM_NET_REVENUE_SNAPSHOT")
    private Long platformNetRevenueSnapshot;

    @Column(name = "FEE_POLICY_ID_SNAPSHOT")
    private Long feePolicyIdSnapshot;

    @Column(name = "FEE_POLICY_APPLIED_AT_SNAPSHOT")
    private LocalDateTime feePolicyAppliedAtSnapshot;

    // 정산 상태(enum 문자열 저장)
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private SettlementStatus status; // READY / COMPLETED / WAIT

    // 4. 화주 정보 (이미지: userId/USER_NO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private Users user; // 정산 대상 화주

    // 5. 정산 관련 타임라인 (이미지: fee_date, fee_complete_date)
    @Column(name = "FEE_DATE")
    private LocalDateTime feeDate; // 정산 생성/요청일

    @Column(name = "FEE_COMPLETE_DATE")
    private LocalDateTime feeCompleteDate; // 실제 결제/정산 완료일

    // 6. 기타 예비 필드 (이미지: Field5, Field6)
    @Column(name = "REMARK_FIELD_5")
    private String field5;

    @Column(name = "REMARK_FIELD_6")
    private String field6;

    // --- 비즈니스 로직 메서드 ---
    
    /**
     * 스냅샷이 있으면 저장된 정산 수익을 우선 사용하고, 없으면 레거시 필드로 폴백합니다.
     */
    public Long calculatePlatformRevenue() {
        if (this.platformNetRevenueSnapshot != null) {
            return this.platformNetRevenueSnapshot;
        }
        if (this.platformGrossRevenueSnapshot != null) {
            return this.platformGrossRevenueSnapshot;
        }
        if (this.totalPrice == null || this.feeRate == null) {
            return 0L;
        }
        return (this.totalPrice * this.feeRate) / 100;
    }

    public void applyPricingSnapshot(TransportPaymentPricingSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        this.baseAmountSnapshot = toLong(snapshot.baseAmount());
        this.shipperFeeRateSnapshot = scaleRate(snapshot.shipperFeeRate());
        this.shipperFeeAmountSnapshot = toLong(snapshot.shipperFeeAmount());
        this.shipperPromoApplied = snapshot.shipperPromoApplied();
        this.shipperChargeAmountSnapshot = toLong(snapshot.shipperChargeAmount());
        this.driverFeeRateSnapshot = scaleRate(snapshot.driverFeeRate());
        this.driverFeeAmountSnapshot = toLong(snapshot.driverFeeAmount());
        this.driverPromoApplied = snapshot.driverPromoApplied();
        this.driverPayoutAmountSnapshot = toLong(snapshot.driverPayoutAmount());
        this.tossFeeRateSnapshot = scaleRate(snapshot.tossFeeRate());
        this.tossFeeAmountSnapshot = toLong(snapshot.tossFeeAmount());
        this.platformGrossRevenueSnapshot = toLong(snapshot.platformGrossRevenue());
        this.platformNetRevenueSnapshot = toLong(snapshot.platformNetRevenue());
        this.feePolicyIdSnapshot = snapshot.feePolicyId();
        this.feePolicyAppliedAtSnapshot = snapshot.feePolicyAppliedAt();

        if (this.totalPrice == null || snapshot.shipperChargeAmount() != null) {
            this.totalPrice = toLong(snapshot.shipperChargeAmount());
        }
        if (this.feeRate == null || snapshot.shipperFeeRate() != null) {
            this.feeRate = toLegacyPercentage(snapshot.shipperFeeRate());
        }
    }

    private Long toLong(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private BigDecimal scaleRate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private Long toLegacyPercentage(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        return rate.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
    
}
