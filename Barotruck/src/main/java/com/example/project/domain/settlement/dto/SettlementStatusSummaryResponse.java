package com.example.project.domain.settlement.dto;

import lombok.Builder;

@Builder
public record SettlementStatusSummaryResponse(
        Long totalAmount,
        Long pendingAmount,
        Long completedAmount,
        Long totalCount,
        Long pendingCount,
        Long completedCount
) {
}
