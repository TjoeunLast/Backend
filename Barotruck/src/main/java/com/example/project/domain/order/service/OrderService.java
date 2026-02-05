package com.example.project.domain.order.service;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // 추가
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 1. 화주: 오더 생성 (C)
     */
    public OrderResponse createOrder(Users user, OrderRequest request) {
        // 엔티티 생성 시 request의 모든 필드를 매핑해줘야 null이 안 나옵니다.
        Order order = Order.builder()
                .user(user)
                .startAddr(request.getStartAddr())
                .startType(request.getStartType())
                .startSchedule(request.getStartSchedule())
                .endAddr(request.getEndAddr())
                .endType(request.getEndType())
                .endSchedule(request.getEndSchedule())
                .cargoContent(request.getCargoContent())
                .loadMethod(request.getLoadMethod())
                .workType(request.getWorkType())
                .tonnage(request.getTonnage())
                .reqCarType(request.getReqCarType())
                .reqTonnage(request.getReqTonnage())
                .driveMode(request.getDriveMode())
                .loadWeight(request.getLoadWeight())
                .basePrice(request.getBasePrice())
                .laborFee(request.getLaborFee())
                .payMethod(request.getPayMethod())
                .totalPrice(request.getBasePrice()) // 우선 기본가로 설정 (로직 추가 가능)
                .status("REQUESTED")
                .build();

        Order savedOrder = orderRepository.save(order);
        return convertToResponse(savedOrder);
    }

    /**
     * 2. 차주: 오더 수락 (U - 배차 완료)
     */
    public void acceptOrder(Long orderId, Long driverNo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 오더가 존재하지 않습니다."));

        if (!"REQUESTED".equals(order.getStatus())) {
            throw new RuntimeException("이미 배차가 완료되었거나 취소된 오더입니다.");
        }

        // 상태 변경 및 차주 번호 할당 (Dirty Checking 활용)
        // Order 엔티티에 @Setter가 없다면 아래 메서드를 엔티티에 추가하는 것을 권장합니다.
        // order.accept(driverNo); 
    }

    /**
     * 3. 차주: 수락 가능한 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders() {
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc("REQUESTED");
        
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 4. 공통: 오더 상세 조회 (R)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));
        return convertToResponse(order);
    }

    
    
    
    /**
     * 엔티티를 OrderResponse DTO로 변환하는 내부 공통 메서드
     */
    private OrderResponse convertToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .startAddr(order.getStartAddr())
                .startType(order.getStartType())
                .startSchedule(order.getStartSchedule())
                .endAddr(order.getEndAddr())
                .endType(order.getEndType())
                .endSchedule(order.getEndSchedule())
                .cargoContent(order.getCargoContent())
                .loadMethod(order.getLoadMethod())
                .workType(order.getWorkType())
                .tonnage(order.getTonnage())
                .reqCarType(order.getReqCarType())
                .reqTonnage(order.getReqTonnage())
                .driveMode(order.getDriveMode())
                .loadWeight(order.getLoadWeight())
                .basePrice(order.getBasePrice())
                .laborFee(order.getLaborFee())
                .payMethod(order.getPayMethod())
                .totalPrice(order.getTotalPrice())
                .driverNo(order.getDriverNo())
                // UserSummary의 static factory method 활용 (비밀번호 자동 제외)
                .user(OrderResponse.UserSummary.from(order.getUser()))
                .build();
    }
}