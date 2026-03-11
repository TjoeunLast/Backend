package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "(?i)shipper|driver", message = "side must be shipper or driver")
    private String side;
}
