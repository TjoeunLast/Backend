package com.example.project.domain.review.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

	private final ReportRepository reportRepository;
    // private final EmailAuthService emailService; // 관리자 알림용 (나중에 주석 해제)

    // [C] 신고 등록
    @Transactional
    public void createReport(Report report) {
        report.setStatus("PENDING"); // 초기 상태 설정
        reportRepository.save(report);
        log.info("신고 접수 완료: ID {}", report.getReportId());
        
        // 비동기로 관리자 알림 발송 로직 호출 가능
        // sendReportNotificationToAdmin(report); 
    }

    // [R] 신고 상세 조회
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다."));
    }

    // [U] 신고 처리 상태 업데이트 (관리자용)
    @Transactional
    public void updateReportStatus(Long reportId, String newStatus) {
        Report report = getReportById(reportId);
        report.setStatus(newStatus);
        log.info("신고 상태 변경: {} -> {}", reportId, newStatus);
        
        // [추가 로직 주석] 
        // 만약 RESOLVED(해결됨)로 변경 시 신고자에게 결과 알림톡 발송 로직 추가 가능
    }

    // [D] 잘못된 신고 데이터 삭제 (관리자용)
    @Transactional
    public void deleteReport(Long reportId) {
        Report report = getReportById(reportId);
        reportRepository.delete(report);
        log.info("신고 내역 삭제 완료: ID {}", reportId);
    }

    // 비동기 알림 (이전 답변 코드 유지)
    @Async
    protected void sendReportNotificationToAdmin(Report report) {
        // ... (주석 처리된 이메일/SMS 발송 로직) // 이거말고 그냥 알림처리로 신고됬다고 할듯
    }
    
 // 상태별 목록 조회 로직 추가
    @Transactional(readOnly = true)
    public List<Report> getReportsByStatus(String status) {
        log.info("신고 목록 조회 요청 - 상태: {}", status);
        return reportRepository.findByStatus(status);
    }
}
