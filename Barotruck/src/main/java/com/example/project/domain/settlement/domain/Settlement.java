package com.example.project.domain.settlement.domain;

import java.time.LocalDateTime;

import com.example.project.domain.order.domain.Order;
import com.example.project.member.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "STATUS")
    private String status; // 정산 상태 (예: READY, COMPLETED, WAIT)

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
     * 최종 플랫폼 순수익 계산 (수수료 수익)
     * 수식: (최종 금액 * 수수료율) / 100
     */
    public Long calculatePlatformRevenue() {
        if (this.totalPrice == null || this.feeRate == null) return 0L;
        return (this.totalPrice * this.feeRate) / 100;
    }
}