package com.example.project.domain.payment.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeePolicyService {

    public record FeeResult(
            BigDecimal feeRate,
            BigDecimal feeAmount,
            BigDecimal netAmount,
            boolean promoApplied
    ) {}

    public FeeResult calculate(BigDecimal amount, Long userLevel, boolean firstPaymentPromoEligible) {
        BigDecimal rate = mapRate(userLevel);

        boolean promoApplied = false;
        if (firstPaymentPromoEligible) {
            rate = new BigDecimal("0.03"); // 첫 결제는 3%처럼 보이게
            promoApplied = true;
        }

        BigDecimal fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal minFee = new BigDecimal("2000.00");
        if (fee.compareTo(minFee) < 0) fee = minFee;

        BigDecimal net = amount.subtract(fee).setScale(2, RoundingMode.HALF_UP);

        return new FeeResult(rate, fee, net, promoApplied);
    }

    private BigDecimal mapRate(Long userLevel) {
        if (userLevel == null) return new BigDecimal("0.05");

        return switch (userLevel.intValue()) {
            case 0, 1 -> new BigDecimal("0.05");
            case 2 -> new BigDecimal("0.04");
            default -> new BigDecimal("0.03");
        };
    }
}