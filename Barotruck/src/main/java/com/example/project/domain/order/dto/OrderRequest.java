package com.example.project.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    // 상차지
    private String startAddr;
    private String startType;
    private String startSchedule; // 추가됨
    private Long startNbhId;

    // 하차지
    private String endAddr;
    private String endType;
    private String endSchedule;   // 추가됨
    private Long endNbhId;

    // 화물 정보
    private String cargoContent;  // 추가됨
    private String loadMethod;    // 추가됨
    private String workType;      // 추가됨
    private BigDecimal tonnage;
    private String reqCarType;    // 추가됨
    private String reqTonnage;    // 추가됨
    private String driveMode;
    private Long loadWeight;

    // 요금 및 결제
    private Long basePrice;
    private Long laborFee;       // 추가됨
    private String payMethod;
}