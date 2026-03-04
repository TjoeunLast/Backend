package com.example.project.domain.order.domain.embedded;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.*;
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

    
    // 상차지 좌표
    private BigDecimal startLat;
    private BigDecimal startLng;

    // 하차지 정보
    private String endAddr;
    private String endPlace;
    private String endType;
    private String endSchedule;
    private String doProvince;

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

    // 오더의 출발/도착 지역 코드
    @Column(name = "START_NBH_ID")
    private Long startNbhId;
    @Column(name = "END_NBH_ID")
    private Long endNbhId;

    // 메모 및 태그
    private String memo;

    @Transient
    private List<String> tag;

    private boolean instant;

}
