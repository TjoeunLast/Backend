package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateFeePolicyRequest {

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level0Rate;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level1Rate;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level2Rate;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal level3PlusRate;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal firstPaymentPromoRate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal minFee;
}

