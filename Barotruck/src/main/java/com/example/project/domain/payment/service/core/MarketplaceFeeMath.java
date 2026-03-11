package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicySideResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MarketplaceFeeMath {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);
    private static final BigDecimal DEFAULT_TOSS_FEE_RATE = new BigDecimal("0.10");

    private MarketplaceFeeMath() {
    }

    static FeeBreakdownPreviewResponse calculate(
            FeePolicyResponse policy,
            BigDecimal baseAmount,
            Long shipperLevel,
            Long driverLevel,
            boolean shipperPromoEligible,
            boolean driverPromoEligible,
            boolean includeTossFee
    ) {
        BigDecimal safeBaseAmount = normalizeAmount(baseAmount);
        long shipperAppliedLevel = normalizeLevelBucket(shipperLevel);
        Long driverAppliedLevel = driverLevel == null ? null : normalizeLevelBucket(driverLevel);
        BigDecimal tossFeeRate = includeTossFee ? resolveTossFeeRate(policy) : BigDecimal.ZERO;
        BigDecimal tossFeeAmount = includeTossFee
                ? roundAmount(safeBaseAmount.multiply(tossFeeRate))
                : ZERO;
        BigDecimal postTossBaseAmount = subtractAmount(safeBaseAmount, tossFeeAmount);

        if (safeBaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return FeeBreakdownPreviewResponse.builder()
                    .baseAmount(safeBaseAmount)
                    .postTossBaseAmount(postTossBaseAmount)
                    .shipperAppliedLevel(shipperAppliedLevel)
                    .driverAppliedLevel(driverAppliedLevel)
                    .shipperFeeRate(BigDecimal.ZERO)
                    .driverFeeRate(BigDecimal.ZERO)
                    .shipperFeeAmount(ZERO)
                    .driverFeeAmount(ZERO)
                    .shipperPromoEligible(shipperPromoEligible)
                    .driverPromoEligible(driverPromoEligible)
                    .shipperPromoApplied(false)
                    .driverPromoApplied(false)
                    .shipperMinFeeApplied(false)
                    .driverMinFeeApplied(false)
                    .shipperChargeAmount(ZERO)
                    .driverPayoutAmount(ZERO)
                    .tossFeeRate(tossFeeRate)
                    .tossFeeAmount(ZERO)
                    .platformGrossRevenue(ZERO)
                    .platformNetRevenue(ZERO)
                    .negativeMargin(false)
                    .policyConfigId(policy == null ? null : policy.policyConfigId())
                    .policyUpdatedAt(policy == null ? null : policy.updatedAt())
                    .build();
        }

        SideBreakdown shipperSide = calculateSide(
                policy,
                postTossBaseAmount,
                shipperAppliedLevel,
                shipperPromoEligible,
                PolicySide.SHIPPER
        );
        SideBreakdown driverSide = driverAppliedLevel == null
                ? SideBreakdown.zero()
                : calculateSide(
                        policy,
                        postTossBaseAmount,
                        driverAppliedLevel,
                        driverPromoEligible,
                        PolicySide.DRIVER
                );
        SideFeePair cappedFees = capSideFees(postTossBaseAmount, shipperSide, driverSide);

        BigDecimal shipperChargeAmount = safeBaseAmount;
        BigDecimal platformGrossRevenue = cappedFees.shipper().feeAmount().add(cappedFees.driver().feeAmount());
        BigDecimal platformNetRevenue = platformGrossRevenue;
        BigDecimal driverPayoutAmount = subtractAmount(postTossBaseAmount, platformGrossRevenue);

        return FeeBreakdownPreviewResponse.builder()
                .baseAmount(safeBaseAmount)
                .postTossBaseAmount(postTossBaseAmount)
                .shipperAppliedLevel(shipperAppliedLevel)
                .driverAppliedLevel(driverAppliedLevel)
                .shipperFeeRate(cappedFees.shipper().feeRate())
                .driverFeeRate(cappedFees.driver().feeRate())
                .shipperFeeAmount(cappedFees.shipper().feeAmount())
                .driverFeeAmount(cappedFees.driver().feeAmount())
                .shipperPromoEligible(shipperPromoEligible)
                .driverPromoEligible(driverPromoEligible)
                .shipperPromoApplied(cappedFees.shipper().promoApplied())
                .driverPromoApplied(cappedFees.driver().promoApplied())
                .shipperMinFeeApplied(cappedFees.shipper().minFeeApplied())
                .driverMinFeeApplied(cappedFees.driver().minFeeApplied())
                .shipperChargeAmount(shipperChargeAmount)
                .driverPayoutAmount(driverPayoutAmount)
                .tossFeeRate(tossFeeRate)
                .tossFeeAmount(tossFeeAmount)
                .platformGrossRevenue(platformGrossRevenue)
                .platformNetRevenue(platformNetRevenue)
                .negativeMargin(platformNetRevenue.compareTo(BigDecimal.ZERO) < 0)
                .policyConfigId(policy == null ? null : policy.policyConfigId())
                .policyUpdatedAt(policy == null ? null : policy.updatedAt())
                .build();
    }

    private static SideBreakdown calculateSide(
            FeePolicyResponse policy,
            BigDecimal baseAmount,
            long appliedLevel,
            boolean promoEligible,
            PolicySide side
    ) {
        BigDecimal rate = resolveLevelRate(policy, appliedLevel, side);
        boolean promoApplied = promoEligible;
        if (promoApplied) {
            rate = normalizeRate(resolvePromoRate(policy, side));
        }

        BigDecimal feeAmount = roundAmount(baseAmount.multiply(rate));
        BigDecimal minFee = roundAmount(policy == null ? null : policy.minFee());
        boolean minFeeApplied = feeAmount.compareTo(minFee) < 0;
        if (minFeeApplied) {
            feeAmount = minFee;
        }

        return new SideBreakdown(rate, feeAmount, promoApplied, minFeeApplied);
    }

    private static SideFeePair capSideFees(
            BigDecimal availableAmount,
            SideBreakdown shipperSide,
            SideBreakdown driverSide
    ) {
        if (availableAmount == null || availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new SideFeePair(shipperSide.withFeeAmount(ZERO), driverSide.withFeeAmount(ZERO));
        }

        BigDecimal totalFeeAmount = shipperSide.feeAmount().add(driverSide.feeAmount());
        if (totalFeeAmount.compareTo(availableAmount) <= 0) {
            return new SideFeePair(shipperSide, driverSide);
        }
        if (totalFeeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new SideFeePair(shipperSide.withFeeAmount(ZERO), driverSide.withFeeAmount(ZERO));
        }

        BigDecimal shipperAmount = roundAmount(
                availableAmount.multiply(shipperSide.feeAmount()).divide(totalFeeAmount, 4, RoundingMode.HALF_UP)
        );
        if (shipperAmount.compareTo(availableAmount) > 0) {
            shipperAmount = availableAmount;
        }
        BigDecimal driverAmount = subtractAmount(availableAmount, shipperAmount);

        return new SideFeePair(
                shipperSide.withFeeAmount(shipperAmount),
                driverSide.withFeeAmount(driverAmount)
        );
    }

    private static BigDecimal resolveLevelRate(FeePolicyResponse policy, long level, PolicySide side) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        FeePolicySideResponse sidePolicy = side == PolicySide.DRIVER ? policy.driverSide() : policy.shipperSide();
        if (sidePolicy == null) {
            return resolveLegacyLevelRate(policy, level);
        }
        if (level <= 0) {
            return normalizeRate(sidePolicy.level0Rate());
        }
        if (level == 1) {
            return normalizeRate(sidePolicy.level1Rate());
        }
        if (level == 2) {
            return normalizeRate(sidePolicy.level2Rate());
        }
        return normalizeRate(sidePolicy.level3PlusRate());
    }

    private static BigDecimal resolveLegacyLevelRate(FeePolicyResponse policy, long level) {
        if (level <= 0) {
            return normalizeRate(policy.level0Rate());
        }
        if (level == 1) {
            return normalizeRate(policy.level1Rate());
        }
        if (level == 2) {
            return normalizeRate(policy.level2Rate());
        }
        return normalizeRate(policy.level3PlusRate());
    }

    private static BigDecimal resolvePromoRate(FeePolicyResponse policy, PolicySide side) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        if (side == PolicySide.DRIVER) {
            return firstNonNull(policy.driverFirstTransportPromoRate(), policy.firstPaymentPromoRate());
        }
        return firstNonNull(policy.shipperFirstPaymentPromoRate(), policy.firstPaymentPromoRate());
    }

    private static BigDecimal resolveTossFeeRate(FeePolicyResponse policy) {
        BigDecimal policyRate = policy == null ? null : policy.tossRate();
        return normalizeRate(policyRate == null ? DEFAULT_TOSS_FEE_RATE : policyRate);
    }

    private static BigDecimal firstNonNull(BigDecimal primary, BigDecimal fallback) {
        return primary != null ? primary : fallback;
    }

    static long normalizeLevelBucket(Long level) {
        if (level == null || level <= 0) {
            return 0L;
        }
        return Math.min(level, 3L);
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return roundAmount(amount);
    }

    private static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal roundAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal subtractAmount(BigDecimal left, BigDecimal right) {
        if (left == null || left.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        BigDecimal safeRight = right == null ? ZERO : right;
        BigDecimal result = left.subtract(safeRight);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return roundAmount(result);
    }

    private record SideBreakdown(
            BigDecimal feeRate,
            BigDecimal feeAmount,
            boolean promoApplied,
            boolean minFeeApplied
    ) {
        private static SideBreakdown zero() {
            return new SideBreakdown(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), ZERO, false, false);
        }

        private SideBreakdown withFeeAmount(BigDecimal updatedFeeAmount) {
            return new SideBreakdown(feeRate, roundAmount(updatedFeeAmount), promoApplied, minFeeApplied);
        }
    }

    private record SideFeePair(
            SideBreakdown shipper,
            SideBreakdown driver
    ) {
    }

    private enum PolicySide {
        SHIPPER,
        DRIVER
    }
}
