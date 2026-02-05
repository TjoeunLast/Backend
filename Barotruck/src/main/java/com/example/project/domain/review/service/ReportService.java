package com.example.project.domain.review.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.dto.ReportRequestDto;
import com.example.project.domain.review.dto.ReportResponseDto;
import com.example.project.domain.review.repository.ReportRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

	private final ReportRepository reportRepository;
	private final OrderRepository orderRepository; // 추가 필요
    private final UsersRepository userRepository;   // 추가 필요
    
    
    @Transactional
    public void createReport(ReportRequestDto dto, Users currentUser) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("해당 오더가 없습니다."));

        // 공통 함수로 신고 대상(Target) 특정
        Long targetId = order.getOpponentId(currentUser.getUserId());
        Users target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("신고 대상자를 찾을 수 없습니다."));

        Report report = Report.builder()
                .order(order)
                .reporter(currentUser)
                .target(target)
                .reportType(dto.getReportType())
                .description(dto.getDescription())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getReportsByStatus(String status) {
        return reportRepository.findByStatus(status).stream()
                .map(ReportResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateReportStatus(Long reportId, String newStatus) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));
        report.setStatus(newStatus);
    }

    @Transactional
    public void deleteReport(Long reportId) {
    	Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));
        
        reportRepository.deleteById(reportId);
    }
    @Transactional
    public void updateReportStatus(Long reportId, String newStatus, Users currentUser) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));
        
        // 본인 확인: 리뷰 작성자의 ID와 현재 로그인한 유저의 ID 비교
    	if (!report.getReporter().getUserId().equals(currentUser.getUserId())) {
    		throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
    	}
    	
        report.setStatus(newStatus);
    }

    @Transactional
    public void deleteReport(Long reportId, Users currentUser) {
    	Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));
        
    	 // 본인 확인: 리뷰 작성자의 ID와 현재 로그인한 유저의 ID 비교
    	if (!report.getReporter().getUserId().equals(currentUser.getUserId())) {
    		throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
    	}
    	reportRepository.deleteById(reportId);
    }
    
    

    // 비동기 알림 (이전 답변 코드 유지)
    @Async
    protected void sendReportNotificationToAdmin(Report report) {
        // ... (주석 처리된 이메일/SMS 발송 로직) // 이거말고 그냥 알림처리로 신고됬다고 할듯
    }
    

}
