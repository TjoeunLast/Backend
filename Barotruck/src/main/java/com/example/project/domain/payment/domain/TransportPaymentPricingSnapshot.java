package com.example.project.domain.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record TransportPaymentPricingSnapshot(
        BigDecimal actualPaidAmount,
        BigDecimal baseAmount,
        BigDecimal shipperFeeRate,
        BigDecimal shipperFeeAmount,
        boolean shipperPromoApplied,
        BigDecimal shipperChargeAmount,
        BigDecimal driverFeeRate,
        BigDecimal driverFeeAmount,
        boolean driverPromoApplied,
        BigDecimal driverPayoutAmount,
        BigDecimal tossFeeRate,
        BigDecimal tossFeeAmount,
        BigDecimal platformGrossRevenue,
        BigDecimal platformNetRevenue,
        Long feePolicyId,
        LocalDateTime feePolicyAppliedAt
) {

    public static TransportPaymentPricingSnapshot from(TransportPayment payment) {
        if (payment == null) {
            return null;
        }

        BigDecimal baseAmount = firstNonNull(
                payment.getBaseAmountSnapshot(),
                payment.getNetAmountSnapshot(),
                payment.getAmount()
        );
        BigDecimal shipperFeeRate = firstNonNull(
                payment.getShipperFeeRateSnapshot(),
                payment.getFeeRateSnapshot(),
                BigDecimal.ZERO
        );
        BigDecimal shipperFeeAmount = firstNonNull(
                payment.getShipperFeeAmountSnapshot(),
                payment.getFeeAmountSnapshot(),
                BigDecimal.ZERO
        );
        BigDecimal shipperChargeAmount = firstNonNull(
                payment.getShipperChargeAmountSnapshot(),
                payment.getAmount(),
                BigDecimal.ZERO
        );
        BigDecimal driverFeeRate = firstNonNull(payment.getDriverFeeRateSnapshot(), BigDecimal.ZERO);
        BigDecimal driverFeeAmount = firstNonNull(payment.getDriverFeeAmountSnapshot(), BigDecimal.ZERO);
        BigDecimal driverPayoutAmount = firstNonNull(
                payment.getDriverPayoutAmountSnapshot(),
                payment.getNetAmountSnapshot(),
                baseAmount
        );
        BigDecimal tossFeeRate = firstNonNull(payment.getTossFeeRateSnapshot(), BigDecimal.ZERO);
        BigDecimal tossFeeAmount = firstNonNull(payment.getTossFeeAmountSnapshot(), BigDecimal.ZERO);
        BigDecimal platformGrossRevenue = firstNonNull(
                payment.getPlatformGrossRevenueSnapshot(),
                shipperFeeAmount.add(driverFeeAmount)
        );
        BigDecimal platformNetRevenue = firstNonNull(
                payment.getPlatformNetRevenueSnapshot(),
                platformGrossRevenue.subtract(tossFeeAmount)
        );

        return new TransportPaymentPricingSnapshot(
                scaleAmount(firstNonNull(payment.getAmount(), shipperChargeAmount)),
                scaleAmount(baseAmount),
                scaleRate(shipperFeeRate),
                scaleAmount(shipperFeeAmount),
                Boolean.TRUE.equals(firstNonNull(payment.getShipperPromoApplied(), payment.isFirstPaymentPromoApplied())),
                scaleAmount(shipperChargeAmount),
                scaleRate(driverFeeRate),
                scaleAmount(driverFeeAmount),
                Boolean.TRUE.equals(payment.getDriverPromoApplied()),
                scaleAmount(driverPayoutAmount),
                scaleRate(tossFeeRate),
                scaleAmount(tossFeeAmount),
                scaleAmount(platformGrossRevenue),
                scaleAmount(platformNetRevenue),
                payment.getFeePolicyIdSnapshot(),
                payment.getFeePolicyAppliedAtSnapshot()
        );
    }

    private static BigDecimal firstNonNull(BigDecimal... values) {
        if (values == null) {
            return BigDecimal.ZERO;
        }
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private static Boolean firstNonNull(Boolean... values) {
        if (values == null) {
            return Boolean.FALSE;
        }
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return Boolean.FALSE;
    }

    private static BigDecimal scaleAmount(BigDecimal value) {
        return firstNonNull(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleRate(BigDecimal value) {
        return firstNonNull(value).setScale(4, RoundingMode.HALF_UP);
    }
}
