package com.example.project.domain.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.AdminControl;
import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.DriverTimeline;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // 추가
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 1. 화주: 오더 생성 (C)
     */
    public OrderResponse createOrder(Users user, OrderRequest request) {
        // 엔티티 생성 로직을 엔티티 내부의 정적 메서드로 위임
        Order order = Order.createOrder(user, request);

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
        // Embedded 객체들을 안전하게 추출 (Null 방지)
        DriverTimeline timeline = order.getDriverTimeline();
        AdminControl admin = order.getAdminControl();
        CancellationInfo cancellation = order.getCancellationInfo();

        return OrderResponse.builder()
                // 1. 주문 기본 정보
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updated(order.getUpdated())

                // 2. 상차지 정보
                .startAddr(order.getStartAddr())
                .startPlace(order.getStartPlace())
                .startType(order.getStartType())
                .startSchedule(order.getStartSchedule())
                .puProvince(order.getPuProvince())
                .startNeighborhoodId(order.getStartNeighborhood() != null ? 
                                     order.getStartNeighborhood().getNeighborhoodId() : null)

                // 3. 하차지 정보
                .endAddr(order.getEndAddr())
                .endPlace(order.getEndPlace())
                .endType(order.getEndType())
                .endSchedule(order.getEndSchedule())
                .doProvince(order.getDoProvince())
                .endNeighborhoodId(order.getEndNeighborhood() != null ? 
                                   order.getEndNeighborhood().getNeighborhoodId() : null)

                // 4. 화물 및 작업 정보
                .cargoContent(order.getCargoContent())
                .loadMethod(order.getLoadMethod())
                .workType(order.getWorkType())
                .tonnage(order.getTonnage())
                .reqCarType(order.getReqCarType())
                .reqTonnage(order.getReqTonnage())
                .driveMode(order.getDriveMode())
                .loadWeight(order.getLoadWeight())

                // 5. 요금 정보
                .basePrice(order.getBasePrice())
                .laborFee(order.getLaborFee())
                .packagingPrice(order.getPackagingPrice())
                .insuranceFee(order.getInsuranceFee())
                .payMethod(order.getPayMethod())

                // 6. 시스템 계산 지표
                .distance(order.getDistance())
                .duration(order.getDuration())

                // 9. 화주(사용자) 정보
                .user(OrderResponse.UserSummary.from(order.getUser()))

                .build();
    }
    
    

}