package com.example.project.domain.settlement.dto;

import lombok.Builder;

@Builder
public record SettlementSummaryResponse(
        Long totalAmount,
        Long platformRevenue,
        Long totalDiscount,
        Long count
) {
}
