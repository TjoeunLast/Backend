package com.example.project.domain.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FEE_POLICY_CONFIGS")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeePolicyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POLICY_ID")
    private Long policyId;

    @Column(name = "LEVEL0_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal level0Rate;

    @Column(name = "LEVEL1_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal level1Rate;

    @Column(name = "LEVEL2_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal level2Rate;

    @Column(name = "LEVEL3_PLUS_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal level3PlusRate;

    @Column(name = "FIRST_PAYMENT_PROMO_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal firstPaymentPromoRate;

    @Column(name = "MIN_FEE", nullable = false, precision = 18, scale = 2)
    private BigDecimal minFee;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

