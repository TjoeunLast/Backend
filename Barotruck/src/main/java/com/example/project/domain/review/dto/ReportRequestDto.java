package com.example.project.domain.review.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportRequestDto {
    private Long orderId;      // 신고와 관련된 오더 ID
    private String reportType; // ACCIDENT, NO_SHOW, RUDE, ETC
    private String description;
}