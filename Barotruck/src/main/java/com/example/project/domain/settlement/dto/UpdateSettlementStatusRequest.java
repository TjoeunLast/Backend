package com.example.project.domain.settlement.dto;

import com.example.project.domain.settlement.domain.SettlementStatus;
import lombok.Getter;

@Getter
public class UpdateSettlementStatusRequest {
    private SettlementStatus status;
    private String adminMemo;
}
