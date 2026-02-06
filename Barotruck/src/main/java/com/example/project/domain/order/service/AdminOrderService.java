package com.example.project.domain.order.service;

import com.example.project.domain.order.domain.AdminControl;
import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    

}