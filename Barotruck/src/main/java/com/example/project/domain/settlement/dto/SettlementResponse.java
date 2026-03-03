package com.example.project.domain.settlement.dto;

import com.example.project.domain.settlement.domain.Settlement;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SettlementResponse(
        Long settlementId,
        Long orderId,
        Long shipperUserId,
        Long driverUserId,
        String driverName, // ★ 추가: 차주 이름
        String bankName,    // ★ 차주 은행명
        String accountNum,  // ★ 차주 계좌번호
        String shipperName,   // 화주 회사명 또는 이름
        String bizNumber,      // 사업자 등록번호
        Long levelDiscount,
        Long couponDiscount,
        Long totalPrice,
        Long feeRate,
        String status,
        LocalDateTime feeDate,
        LocalDateTime feeCompleteDate
) {
    public static SettlementResponse from(
    		Settlement settlement, 
    		String driverName, 
    		String bankName, 
    		String accountNum,
    		String shipperName,
    		String bizRegNum
    		) {
        return SettlementResponse.builder()
                .settlementId(settlement.getId())
                .orderId(settlement.getOrder() != null ? settlement.getOrder().getOrderId() : null)
                .shipperUserId(settlement.getUser() != null ? settlement.getUser().getUserId() : null)
                .driverUserId(settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null)
                // ★ 아래 줄을 추가하여 실명을 빌더에 넣어줍니다.
                .driverName(driverName)
                .bankName(bankName)
                .accountNum(accountNum)
                .shipperName(shipperName)
                .bizNumber(bizRegNum)
                .levelDiscount(settlement.getLevelDiscount())
                .couponDiscount(settlement.getCouponDiscount())
                .totalPrice(settlement.getTotalPrice())
                .feeRate(settlement.getFeeRate())
                .status(settlement.getStatus() == null ? null : settlement.getStatus().name())
                .feeDate(settlement.getFeeDate())
                .feeCompleteDate(settlement.getFeeCompleteDate())
                .build();
    }
}
