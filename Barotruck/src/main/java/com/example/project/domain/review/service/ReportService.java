package com.example.project.domain.review.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.dto.ReportRequestDto;
import com.example.project.domain.review.dto.ReportResponseDto;
import com.example.project.domain.review.repository.ReportRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository; // 추가 필요
    private final UsersRepository userRepository; // 추가 필요
    private final NotificationService notificationService;

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

        Report saved = reportRepository.save(report);
        sendReportNotificationToAdmins(saved);
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
        sendReportNotificationSafely(
                report.getReporter(),
                "신고 처리 상태 변경",
                "신고 상태가 '" + newStatus + "'(으)로 변경되었습니다.",
                report.getReportId()
        );
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

        // 본인 확인: 신고 작성자의 ID와 현재 로그인한 유저의 ID 비교
        if (!report.getReporter().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("본인이 작성한 신고만 수정할 수 있습니다.");
        }

        report.setStatus(newStatus);
    }

    @Transactional
    public void deleteReport(Long reportId, Users currentUser) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));

        // 본인 확인: 신고 작성자의 ID와 현재 로그인한 유저의 ID 비교
        if (!report.getReporter().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("본인이 작성한 신고만 삭제할 수 있습니다.");
        }
        reportRepository.deleteById(reportId);
    }

    // 비동기 알림 (이전 답변 코드 유지)
    @Async
    protected void sendReportNotificationToAdmins(Report report) {
        userRepository.findAllByRole(Role.ADMIN, Sort.by(Sort.Direction.ASC, "userId"))
                .forEach(admin -> sendReportNotificationSafely(
                        admin,
                        "신고 접수",
                        String.format("주문 %d 에 대한 신고가 접수되었습니다.", report.getOrder().getOrderId()),
                        report.getReportId()
                ));
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReportResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getMyReports(Users currentUser) {
        return reportRepository.findByReporter_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(ReportResponseDto::new)
                .collect(Collectors.toList());
    }

    private void sendReportNotificationSafely(Users recipient, String title, String body, Long targetId) {
        if (recipient == null) {
            return;
        }
        try {
            notificationService.sendNotification(recipient, "REPORT", title, body, targetId);
        } catch (Exception e) {
            log.warn("신고 알림 발송 실패: {}", e.getMessage());
        }
    }

}
