package com.example.project.domain.review.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 1. 신고 접수 (화주/차주용)
    @PostMapping
    public ResponseEntity<Boolean> createReport(@RequestBody Report report) {
        reportService.createReport(report);
        return ResponseEntity.ok(true);
    }

    // 2. 신고 상태 업데이트 (관리자용 - 나중에 Role 체크 추가)
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<Boolean> updateReportStatus(
            @PathVariable("reportId") Long reportId,
            @RequestParam("status") String status
    ) {
        // [주석] SecurityContextHolder를 사용하여 ADMIN 권한인지 확인하는 로직 추가 예정
        reportService.updateReportStatus(reportId, status);
        return ResponseEntity.ok(true);
    }

    // 3. 상태별 신고 목록 조회 (관리자용)
    @GetMapping("/status")
    public ResponseEntity<List<Report>> getReportsByStatus(@RequestParam("status") String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    // 4. 신고 내역 삭제
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Boolean> deleteReport(@PathVariable("reportId") Long reportId) {
        reportService.deleteReport(reportId);
        return ResponseEntity.ok(true);
    }
}
