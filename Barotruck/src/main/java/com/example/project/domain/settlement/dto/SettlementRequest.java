package com.example.project.domain.settlement.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SettlementRequest {
    private Long orderId;
    private Long couponDiscount; // 쿠폰 할인액
    private Long levelDiscount;  // 등급 할인액
}