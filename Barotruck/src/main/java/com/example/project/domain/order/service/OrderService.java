package com.example.project.domain.order.service;

import java.time.LocalDateTime;
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
        
        order.assignDriver(driverNo, "ACCEPTED");
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
     * 관리자: 오더 강제 배차
     */
    public void forceAllocateOrder(Long orderId, Long driverId, String adminEmail, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        // 1. 오더 상태 및 차주 변경
        order.assignDriver(driverId, "ALLOCATED");

        // 2. 관리자 제어 기록 생성 및 연관관계 설정
        AdminControl control = AdminControl.builder()
                .isForced("Y")
                .paidAdmin(adminEmail)
                .paidReason(reason)
                .allocated(LocalDateTime.now())
                .build();
        
        order.setAdminControl(control); // Order 내부의 편의 메서드 활용
        orderRepository.save(order);
    }

    /**
     * 관리자: 오더 강제 취소
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
                .cancelledBy("ADMIN")
                .build();

        order.setCancellationInfo(cancelInfo);
        orderRepository.save(order);
    }

    /**
     * 관리자: 전체 오더 목록 조회 (페이징 처리를 권장하지만, 일단 리스트로 구현)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 관리자: 모든 취소된 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getCancelledOrdersForAdmin() {
        List<String> cancelStatuses = List.of("CANCELLED_BY_USER", "CANCELLED_BY_DRIVER", "CANCELLED_BY_ADMIN");
        return orderRepository.findByStatusInOrderByCreatedAtDesc(cancelStatuses).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 오더 취소 공통 로직
     */
    public void cancelOrder(Long orderId, String cancelReason, Users currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        String role = determineCancelRole(order, currentUser); // 취소 주체 판별
        
        // 1. 상태별 취소 가능 여부 체크
        validateCancelCondition(order, role);

        // 2. 오더 상태 업데이트
        String newStatus = "CANCELLED_BY_" + role;
        order.cancelOrder(newStatus); //

        // 3. 취소 정보 생성 및 연관관계 설정
        CancellationInfo cancelInfo = CancellationInfo.builder()
                .order(order)
                .cancelReason(cancelReason)
                .cancelledAt(LocalDateTime.now())
                .cancelledBy(role)
                .build();
        
        order.setCancellationInfo(cancelInfo); //
        orderRepository.save(order);
    }

    private String determineCancelRole(Order order, Users user) {
        if (user.getRole().name().equals("ADMIN")) return "ADMIN";
        if (order.getUser().getUserId().equals(user.getUserId())) return "USER";
        if (order.getDriverNo() != null && order.getDriverNo().equals(user.getUserId())) return "DRIVER";
        throw new RuntimeException("취소 권한이 없습니다.");
    }

    private void validateCancelCondition(Order order, String role) {
        if (role.equals("ADMIN")) return; // 관리자는 무조건 가능
        
        // 이미 완료된 오더는 취소 불가
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("이미 완료된 오더는 취소할 수 없습니다.");
        }
        
        // 운송 중(IN_TRANSIT)인 경우 일반 취소 불가 (관리자에게 문의해야 함)
        if ("IN_TRANSIT".equals(order.getStatus())) {
            throw new RuntimeException("운행 중인 오더는 직접 취소가 불가능합니다. 고객센터에 문의하세요.");
        }
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

                
                .status(order.getStatus())
                // 9. 화주(사용자) 정보
                .cancellation(OrderResponse.CancellationSummary.from(order.getCancellationInfo()))
                .user(OrderResponse.UserSummary.from(order.getUser()))

                .build();
    }
    
    

}