package com.example.project.domain.order.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.AdminControl;
import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.Order.ProvinceAnalysisResponse;
import com.example.project.domain.order.domain.Order.ProvinceStatResponse;
import com.example.project.domain.order.domain.Order.RouteStatResponse;
import com.example.project.domain.order.domain.Order.RouteStatisticsResponse;
import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public void forceAllocateOrder(Long orderId, Long driverId, String adminEmail, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        order.assignDriver(driverId, "ALLOCATED"); // 도메인 메서드 호출

        AdminControl control = AdminControl.builder()
                .isForced("Y")
                .paidAdmin(adminEmail)
                .paidReason(reason)
                .allocated(LocalDateTime.now())
                .build();
        
        order.setAdminControl(control); // 연관관계 설정 시 Dirty Checking으로 자동 반영
    }

    /**
     * 관리자: 오더 강제 취소 (컨트롤러의 호출에 맞춰 인자 수정: orderId, adminEmail, reason)
     */
    public void adminCancelOrder(Long orderId, String adminEmail, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        // 1. 오더 상태 변경
        order.cancelOrder("CANCELLED_BY_ADMIN");

        // 2. 취소 정보 기록
        CancellationInfo cancelInfo = CancellationInfo.builder()
                .cancelReason(reason)
                .cancelledAt(LocalDateTime.now())
                .cancelledBy("ADMIN (" + adminEmail + ")") // 누가 취소했는지 기록에 포함
                .build();

        order.setCancellationInfo(cancelInfo);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCancelledOrdersForAdmin() {
        List<String> cancelStatuses = List.of("CANCELLED_BY_USER", "CANCELLED_BY_DRIVER", "CANCELLED_BY_ADMIN");
        return orderRepository.findByStatusInOrderByCreatedAtDesc(cancelStatuses).stream()
                .map(OrderResponse::from)
                .toList();
    }
    
    
    /**
     * 관리자 대시보드 요약 정보 조회
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRequested", orderRepository.countByStatus("REQUESTED"));
        summary.put("totalInTransit", orderRepository.countByStatus("IN_TRANSIT"));
        summary.put("totalCompleted", orderRepository.countByStatus("COMPLETED"));
        
        // 오늘 발생한 취소 건수
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        long todayCancelled = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startOfDay, endOfDay)
                .stream()
                .filter(o -> o.getStatus().startsWith("CANCELLED"))
                .count();
        summary.put("todayCancelled", todayCancelled);
        
        return summary;
    }

    /**
     * 관리자: 기사별 운송 타임라인 지표 확인 (정상 운송 여부 체크)
     */
    public List<OrderResponse> getLongDurationOrders() {
        // 예상 소요시간(duration)보다 실제 걸린 시간이 훨씬 긴 오더들을 필터링하여 반환하는 로직 등 구현 가능
        return orderRepository.findAll().stream()
                .filter(o -> o.getDuration() != null && o.getDriverTimeline() != null)
                // 필요한 분석 로직 추가
                .map(OrderResponse::from)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<RouteStatisticsResponse> getRouteStatistics() {
        List<Object[]> results = orderRepository.countOrdersByRoute();
        
        return results.stream()
                .map(result -> new RouteStatisticsResponse(
                        (String) result[0], // puProvince
                        (String) result[1], // doProvince
                        (Long) result[2]    // count
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 지역별 출발/도착 통합 통계 조회
     */
    public Map<String, List<ProvinceStatResponse>> getProvinceStatistics() {
        Map<String, List<ProvinceStatResponse>> stats = new HashMap<>();

        // 출발지 통계 가공
        stats.put("pickupStats", orderRepository.countOrdersByPickupProvince().stream()
                .map(res -> new ProvinceStatResponse((String) res[0], (Long) res[1]))
                .toList());

        // 도착지 통계 가공
        stats.put("dropoffStats", orderRepository.countOrdersByDropoffProvince().stream()
                .map(res -> new ProvinceStatResponse((String) res[0], (Long) res[1]))
                .toList());

        return stats;
    }
    
    
    public Map<String, List<ProvinceStatResponse>> getProvinceStatsByPeriod(String period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = switch (period.toLowerCase()) {
            case "day" -> end.minusDays(1);
            case "week" -> end.minusWeeks(1);
            case "month" -> end.minusMonths(1);
            default -> end.minusDays(1); // 기본값은 최근 24시간
        };

        Map<String, List<ProvinceStatResponse>> stats = new HashMap<>();

        stats.put("pickupStats", orderRepository.countPickupByPeriod(start, end).stream()
                .map(res -> new ProvinceStatResponse((String) res[0], (Long) res[1]))
                .toList());

        stats.put("dropoffStats", orderRepository.countDropoffByPeriod(start, end).stream()
                .map(res -> new ProvinceStatResponse((String) res[0], (Long) res[1]))
                .toList());

        return stats;
    }
    
    public Map<String, Object> getComprehensiveStats(String period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = getStartTime(period, end);

        Map<String, Object> stats = new HashMap<>();

        // 1. 노선별 통계 (쌍으로 묶은 것)
        stats.put("routeStats", orderRepository.countRouteStatsByPeriod(start, end).stream()
                .map(res -> new RouteStatResponse((String) res[0], (String) res[1], (Long) res[2]))
                .toList());

        // 2. 출발지별 통계 (건수 + 매출)
        stats.put("pickupStats", orderRepository.countPickupStatsByPeriod(start, end).stream()
                .map(res -> new ProvinceAnalysisResponse((String) res[0], (Long) res[1], (Long) res[2]))
                .toList());

        // 3. 도착지별 통계 (건수 + 매출)
        stats.put("dropoffStats", orderRepository.countDropoffStatsByPeriod(start, end).stream()
                .map(res -> new ProvinceAnalysisResponse((String) res[0], (Long) res[1], (Long) res[2]))
                .toList());

        return stats;
    }

    private LocalDateTime getStartTime(String period, LocalDateTime end) {
        return switch (period.toLowerCase()) {
            case "day" -> end.minusDays(1);
            case "week" -> end.minusWeeks(1);
            case "month" -> end.minusMonths(1);
            case "year" -> end.minusYears(1);
            default -> end.minusDays(1);
        };
    }

}