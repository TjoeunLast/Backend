package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateFeePolicyRequest {

    @Valid
    private FeePolicySideRequest shipperSide;

    @Valid
    private FeePolicySideRequest driverSide;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal shipperFirstPaymentPromoRate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal driverFirstTransportPromoRate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal tossRate;

    @DecimalMin("0.00")
    private BigDecimal minFee;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level0Rate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level1Rate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level2Rate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level3PlusRate;

    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal firstPaymentPromoRate;

    public boolean hasAnyUpdates() {
        return minFee != null
                || shipperFirstPaymentPromoRate != null
                || driverFirstTransportPromoRate != null
                || tossRate != null
                || hasLegacyShipperRateUpdates()
                || (shipperSide != null && shipperSide.hasAnyRate())
                || (driverSide != null && driverSide.hasAnyRate());
    }

    public boolean hasLegacyShipperRateUpdates() {
        return level0Rate != null
                || level1Rate != null
                || level2Rate != null
                || level3PlusRate != null
                || firstPaymentPromoRate != null;
    }
}
