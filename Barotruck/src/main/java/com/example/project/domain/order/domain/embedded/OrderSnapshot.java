package com.example.project.domain.order.domain.embedded;

import java.math.BigDecimal;

import com.example.project.global.neighborhood.Neighborhood;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.example.project.global.neighborhood.Neighborhood;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderSnapshot {

    // 상차지 정보
    private String startAddr;
    private String startPlace;
    private String startType;
    private String startSchedule;
    private String puProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "START_NBH_ID", nullable=true)
    private Neighborhood startNeighborhood;

    // 하차지 정보
    private String endAddr;
    private String endPlace;
    private String endType;
    private String endSchedule;
    private String doProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "END_NBH_ID", nullable=true)
    private Neighborhood endNeighborhood;

    // 화물 및 작업 정보
    private String cargoContent;
    private String loadMethod;
    private String workType;
    private BigDecimal tonnage;
    private String reqCarType;
    private String reqTonnage;
    private String driveMode;
    private Long loadWeight;

    // 요금 및 결제 정보
    private Long basePrice;
    private Long laborFee;
    private Long packagingPrice;
    private Long insuranceFee;
    private String payMethod;
    
    private boolean instant;
}
