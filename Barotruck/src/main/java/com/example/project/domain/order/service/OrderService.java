package com.example.project.domain.order.service;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    // 1. 화주: 오더 생성 (C)
    public Order createOrder(Users user, OrderRequest request) {
        // 빌더 패턴을 사용하여 엔티티 생성 (엔티티에 @Builder 추가 필요)
        // 여기서는 간단히 로직 흐름만 기술
        Order order = Order.builder()
                .user(user)
                .startAddr(request.getStartAddr())
                .status("REQUESTED")
                .totalPrice(request.getBasePrice()) // 수수료 로직 등 추가 가능
                .build();
        return orderRepository.save(order);
    }

    // 2. 차주: 오더 수락 (U - 배차 완료)
    public void acceptOrder(Long orderId, Long driverNo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 오더가 존재하지 않습니다."));

        if (!"REQUESTED".equals(order.getStatus())) {
            throw new RuntimeException("이미 배차가 완료되었거나 취소된 오더입니다.");
        }

        // 차주 정보 업데이트 및 상태 변경
        // 엔티티에 세터 대신 updateStatus 같은 메서드 권장
        // order.assignDriver(driverNo);
    }

    // 3. 공통: 오더 상세 조회 (R)
    @Transactional(readOnly = true)
    public Order getOrderDetail(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));
    }

    /**
     * 차주가 수락 가능한(매칭 대기 중인) 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Order> getAvailableOrders() {
        // "REQUESTED" 상태인 오더들만 필터링하여 반환
        return orderRepository.findByStatusOrderByCreatedAtDesc("REQUESTED");
    }
}