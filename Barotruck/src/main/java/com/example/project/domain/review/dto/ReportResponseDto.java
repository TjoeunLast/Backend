package com.example.project.domain.review.dto;

import com.example.project.domain.review.domain.Report;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ReportResponseDto {
    private Long reportId;
    private Long orderId;
    private String reporterNickname;
    private String targetNickname;
    private String reportType;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public ReportResponseDto(Report report) {
        this.reportId = report.getReportId();
        this.orderId = report.getOrder().getOrderId();
        this.reporterNickname = report.getReporter().getNickname();
        this.targetNickname = report.getTarget().getNickname();
        this.reportType = report.getReportType();
        this.description = report.getDescription();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
    }
}
