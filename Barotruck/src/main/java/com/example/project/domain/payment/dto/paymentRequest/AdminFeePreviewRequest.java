package com.example.project.domain.payment.dto.paymentRequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminFeePreviewRequest {

    @NotNull
    @DecimalMin("0")
    private BigDecimal baseAmount;

    private Long shipperLevel;

    private Long driverLevel;

    private Boolean shipperPromoApplied;

    private Boolean driverPromoApplied;

    private Boolean includeTossFee;
}
