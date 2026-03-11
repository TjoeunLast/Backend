package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeePolicySideRequest {

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

    public boolean hasAnyRate() {
        return level0Rate != null
                || level1Rate != null
                || level2Rate != null
                || level3PlusRate != null;
    }
}
