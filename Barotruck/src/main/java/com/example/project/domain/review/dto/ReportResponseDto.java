package com.example.project.domain.review.dto;

import com.example.project.domain.review.domain.Report;

import jakarta.persistence.Column;
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
    private String type; // REPORT, DISCUSS
    private String email; // email
    private String title; // title
    
    
    
    public ReportResponseDto(Report report) {
        this.reportId = report.getReportId();
        this.orderId = report.getOrder().getOrderId();
        this.reporterNickname = report.getReporter().getNickname();
        this.targetNickname = report.getTarget().getNickname();
        this.reportType = report.getReportType();
        this.description = report.getDescription();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
        this.type = report.getType();
        this.email = report.getEmail();
        this.title = report.getTitle();
    }
}
