package com.example.project.domain.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private String startAddr;
    private String startType;
    private Long startNbhId; // Neighborhood ID
    private String endAddr;
    private String endType;
    private Long endNbhId;   // Neighborhood ID
    private BigDecimal tonnage;
    private Long loadWeight;
    private String driveMode;
    private Long basePrice;
    private String payMethod;
}
