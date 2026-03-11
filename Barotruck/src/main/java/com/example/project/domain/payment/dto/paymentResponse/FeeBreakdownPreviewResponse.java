package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FeeBreakdownPreviewResponse(
        String previewMode,
        PaymentProvider paymentProvider,
        BigDecimal baseAmount,
        BigDecimal postTossBaseAmount,
        Long shipperAppliedLevel,
        Long driverAppliedLevel,
        BigDecimal shipperFeeRate,
        BigDecimal driverFeeRate,
        BigDecimal shipperFeeAmount,
        BigDecimal driverFeeAmount,
        boolean shipperPromoEligible,
        Boolean driverPromoEligible,
        boolean shipperPromoApplied,
        Boolean driverPromoApplied,
        boolean shipperMinFeeApplied,
        Boolean driverMinFeeApplied,
        BigDecimal shipperChargeAmount,
        BigDecimal driverPayoutAmount,
        BigDecimal tossFeeRate,
        BigDecimal tossFeeAmount,
        BigDecimal platformGrossRevenue,
        BigDecimal platformNetRevenue,
        boolean negativeMargin,
        Long policyConfigId,
        LocalDateTime policyUpdatedAt
) {
}
