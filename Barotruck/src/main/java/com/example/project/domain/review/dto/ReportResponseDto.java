package com.example.project.domain.review.dto;

import com.example.project.domain.review.domain.Report;
import com.example.project.member.dto.UserResponseDto;

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
    private UserResponseDto reporterUser;
    private UserResponseDto targetUser;
    
    
    
    public ReportResponseDto(Report report) {
        this.reportId = report.getReportId();
        this.orderId = report.getOrder() != null ? report.getOrder().getOrderId() : null;
        this.reporterNickname = report.getReporter() != null ? report.getReporter().getNickname() : null;
        this.targetNickname = report.getTarget() != null ? report.getTarget().getNickname() : null;
        this.reportType = report.getReportType();
        this.description = report.getDescription();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
        this.type = report.getType();
        this.email = report.getEmail();
        this.title = report.getTitle();
        this.reporterUser = report.getReporter() != null ? UserResponseDto.from(report.getReporter()) : null;
        this.targetUser = report.getTarget() != null ? UserResponseDto.from(report.getTarget()) : null;
    }
}
