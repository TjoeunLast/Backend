package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLevelFeeRequest {

    @NotNull
    @Min(0)
    private Long level;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal rate;
}
