package com.example.project.domain.review.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.review.dto.ReportRequestDto;
import com.example.project.domain.review.dto.ReportResponseDto;
import com.example.project.domain.review.service.ReportService;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

 // 1. 신고 접수 (DTO 사용)
    @PostMapping
    public ResponseEntity<Boolean> createReport(
            @RequestBody ReportRequestDto dto,
            @AuthenticationPrincipal Users currentUser
    ) {
        reportService.createReport(dto, currentUser);
        return ResponseEntity.ok(true);
    }



    // 3. 상태별 신고 목록 조회 (ResponseDto 반환)
    @GetMapping("/status")
    public ResponseEntity<List<ReportResponseDto>> getReportsByStatus(
    		@RequestParam("status") String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    // 2. 신고 상태 업데이트 (관리자)
    @PatchMapping("/admin/{reportId}/status")
    public ResponseEntity<Boolean> updateReportStatus(
            @PathVariable("reportId") Long reportId,
            @RequestParam("status") String status,
            @AuthenticationPrincipal Users currentUser
    ) {
    	if(currentUser.getRole() != Role.ADMIN) {
    	    return ResponseEntity.ok(false);
    	}
        reportService.updateReportStatus(reportId, status);
        return ResponseEntity.ok(true);
    }
    
    @DeleteMapping("/admin/{reportId}")
    public ResponseEntity<Boolean> deleteReport(
    		@PathVariable("reportId") Long reportId,
            @AuthenticationPrincipal Users currentUser
    		) {
    	if(currentUser.getRole() != Role.ADMIN) {
    	    return ResponseEntity.ok(false);
    	}
        reportService.deleteReport(reportId);
        return ResponseEntity.ok(true);
    }
    
 // 2. 신고 상태 업데이트 (사용자)
    @PatchMapping("/my/{reportId}/status")
    public ResponseEntity<Boolean> updateMyReportStatus(
            @PathVariable("reportId") Long reportId,
            @RequestParam("status") String status,
            @AuthenticationPrincipal Users currentUser
    ) {
        reportService.updateReportStatus(reportId, status, currentUser);
        return ResponseEntity.ok(true);
    }
    
    // 사용자
    @DeleteMapping("/my/{reportId}")
    public ResponseEntity<Boolean> deleteMyReport(
    		@PathVariable("reportId") Long reportId,
            @AuthenticationPrincipal Users currentUser
    		) {
        reportService.deleteReport(reportId, currentUser);
        return ResponseEntity.ok(true);
    }
    
}
