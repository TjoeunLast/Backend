package com.example.project.domain.settlement.dto;

import lombok.Builder;

@Builder
public record SettlementRegionStatResponse(
        String province,
        Long totalAmount,
        Long count
) {
}
