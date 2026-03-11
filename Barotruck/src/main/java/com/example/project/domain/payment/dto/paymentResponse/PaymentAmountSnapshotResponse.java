package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Builder
public record PaymentAmountSnapshotResponse(
        BigDecimal baseAmount,
        BigDecimal shipperChargeAmount,
        BigDecimal shipperFeeRate,
        BigDecimal shipperFeeAmount,
        Boolean shipperPromoApplied,
        BigDecimal driverFeeRate,
        BigDecimal driverFeeAmount,
        Boolean driverPromoApplied,
        BigDecimal driverPayoutAmount,
        BigDecimal tossFeeRate,
        BigDecimal tossFeeAmount,
        BigDecimal platformGrossRevenue,
        BigDecimal platformNetRevenue,
        Long feePolicyId,
        LocalDateTime feePolicyAppliedAt
) {

    private static final BigDecimal ZERO_RATE = new BigDecimal("0.0000");
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final BigDecimal TOSS_RATE = new BigDecimal("0.1000");

    public static PaymentAmountSnapshotResponse from(TransportPayment payment) {
        return merge(payment, null, null);
    }

    public static PaymentAmountSnapshotResponse from(FeeInvoiceItem item) {
        return merge(null, item, null);
    }

    public static PaymentAmountSnapshotResponse from(DriverPayoutItem item) {
        return merge(null, null, item);
    }

    public static PaymentAmountSnapshotResponse merge(
            TransportPayment payment,
            FeeInvoiceItem invoiceItem,
            DriverPayoutItem payoutItem
    ) {
        BigDecimal shipperChargeAmount = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getShipperChargeAmount(),
                payoutItem == null ? null : payoutItem.getShipperChargeAmount(),
                payment == null ? null : payment.getShipperChargeAmountSnapshot(),
                payment == null ? null : payment.getAmount()
        );
        BigDecimal shipperFeeRate = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getShipperFeeRate(),
                payoutItem == null ? null : payoutItem.getShipperFeeRate(),
                payment == null ? null : payment.getShipperFeeRateSnapshot(),
                payment == null ? null : payment.getFeeRateSnapshot()
        );
        BigDecimal shipperFeeAmount = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getShipperFeeAmount(),
                payoutItem == null ? null : payoutItem.getShipperFeeAmount(),
                payment == null ? null : payment.getShipperFeeAmountSnapshot(),
                payment == null ? null : payment.getFeeAmountSnapshot()
        );
        Boolean shipperPromoApplied = firstNonNull(
                payment == null ? null : payment.getShipperPromoApplied(),
                payment == null ? null : payment.isFirstPaymentPromoApplied()
        );
        BigDecimal driverFeeAmount = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getDriverFeeAmount(),
                payoutItem == null ? null : payoutItem.getDriverFeeAmount(),
                payment == null ? null : payment.getDriverFeeAmountSnapshot()
        );
        BigDecimal driverPayoutAmount = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getDriverPayoutAmount(),
                payoutItem == null ? null : payoutItem.getDriverPayoutAmount(),
                payment == null ? null : payment.getDriverPayoutAmountSnapshot(),
                payment == null ? null : payment.getNetAmountSnapshot()
        );
        BigDecimal tossFeeRate = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getTossFeeRate(),
                payoutItem == null ? null : payoutItem.getTossFeeRate(),
                payment == null ? null : payment.getTossFeeRateSnapshot(),
                defaultTossRate(payment == null ? null : payment.getMethod())
        );
        BigDecimal tossFeeAmount = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getTossFeeAmount(),
                payoutItem == null ? null : payoutItem.getTossFeeAmount(),
                payment == null ? null : payment.getTossFeeAmountSnapshot(),
                deriveTossFeeAmount(
                        firstNonNull(
                                payment == null ? null : payment.getBaseAmountSnapshot(),
                                shipperChargeAmount
                        ),
                        defaultTossRate(payment == null ? null : payment.getMethod())
                )
        );
        BigDecimal baseAmount = firstNonNull(
                payment == null ? null : payment.getBaseAmountSnapshot(),
                deriveBaseAmount(
                        shipperChargeAmount,
                        shipperFeeAmount,
                        driverPayoutAmount,
                        driverFeeAmount,
                        tossFeeAmount
                ),
                payment == null ? null : payment.getAmount()
        );
        Boolean driverPromoApplied = firstNonNull(
                payment == null ? null : payment.getDriverPromoApplied(),
                Boolean.FALSE
        );
        BigDecimal driverFeeRate = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getDriverFeeRate(),
                payoutItem == null ? null : payoutItem.getDriverFeeRate(),
                payment == null ? null : payment.getDriverFeeRateSnapshot(),
                deriveRate(driverFeeAmount, baseAmount)
        );
        BigDecimal platformGrossRevenue = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getPlatformGrossRevenue(),
                payoutItem == null ? null : payoutItem.getPlatformGrossRevenue(),
                payment == null ? null : payment.getPlatformGrossRevenueSnapshot(),
                sum(shipperFeeAmount, driverFeeAmount)
        );
        BigDecimal platformNetRevenue = firstNonNull(
                invoiceItem == null ? null : invoiceItem.getPlatformNetRevenue(),
                payoutItem == null ? null : payoutItem.getPlatformNetRevenue(),
                payment == null ? null : payment.getPlatformNetRevenueSnapshot(),
                subtract(platformGrossRevenue, tossFeeAmount)
        );
        Long feePolicyId = payment == null ? null : payment.getFeePolicyIdSnapshot();
        LocalDateTime feePolicyAppliedAt = payment == null ? null : payment.getFeePolicyAppliedAtSnapshot();

        return PaymentAmountSnapshotResponse.builder()
                .baseAmount(baseAmount)
                .shipperChargeAmount(shipperChargeAmount)
                .shipperFeeRate(shipperFeeRate)
                .shipperFeeAmount(shipperFeeAmount)
                .shipperPromoApplied(shipperPromoApplied)
                .driverFeeRate(driverFeeRate)
                .driverFeeAmount(driverFeeAmount)
                .driverPromoApplied(driverPromoApplied)
                .driverPayoutAmount(driverPayoutAmount)
                .tossFeeRate(tossFeeRate)
                .tossFeeAmount(tossFeeAmount)
                .platformGrossRevenue(platformGrossRevenue)
                .platformNetRevenue(platformNetRevenue)
                .feePolicyId(feePolicyId)
                .feePolicyAppliedAt(feePolicyAppliedAt)
                .build();
    }

    private static BigDecimal deriveBaseAmount(
            BigDecimal shipperChargeAmount,
            BigDecimal shipperFeeAmount,
            BigDecimal driverPayoutAmount,
            BigDecimal driverFeeAmount,
            BigDecimal tossFeeAmount
    ) {
        if (
                shipperChargeAmount != null &&
                driverPayoutAmount != null &&
                shipperFeeAmount != null &&
                driverFeeAmount != null &&
                tossFeeAmount != null
        ) {
            BigDecimal recomposed = driverPayoutAmount
                    .add(shipperFeeAmount)
                    .add(driverFeeAmount)
                    .add(tossFeeAmount)
                    .setScale(2, RoundingMode.HALF_UP);
            if (recomposed.compareTo(shipperChargeAmount.setScale(2, RoundingMode.HALF_UP)) == 0) {
                return shipperChargeAmount.setScale(2, RoundingMode.HALF_UP);
            }
        }
        if (shipperChargeAmount != null && shipperFeeAmount != null) {
            return shipperChargeAmount.subtract(shipperFeeAmount).setScale(2, RoundingMode.HALF_UP);
        }
        if (
                driverPayoutAmount != null &&
                (shipperFeeAmount != null || driverFeeAmount != null || tossFeeAmount != null)
        ) {
            return driverPayoutAmount
                    .add(safe(shipperFeeAmount))
                    .add(safe(driverFeeAmount))
                    .add(safe(tossFeeAmount))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private static BigDecimal deriveRate(BigDecimal feeAmount, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (feeAmount == null || feeAmount.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_RATE;
        }
        return feeAmount.divide(baseAmount, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal deriveTossFeeAmount(BigDecimal baseAmount, BigDecimal tossFeeRate) {
        if (baseAmount == null || tossFeeRate == null) {
            return null;
        }
        return baseAmount.multiply(tossFeeRate).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultTossRate(PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.CARD) {
            return TOSS_RATE;
        }
        return ZERO_RATE;
    }

    private static BigDecimal sum(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return null;
        }
        return safe(left).add(safe(right)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.subtract(right).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO_AMOUNT : value;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
