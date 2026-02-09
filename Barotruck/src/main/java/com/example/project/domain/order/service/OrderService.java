package com.example.project.domain.order.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.project.domain.order.domain.FarePolicy;
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.repository.FareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.AdminControl;
import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // 추가
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.DriverRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository; // 드라이버 정보 조회를 위한 레포지토리
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
        System.out.println("--------------------------------------");
        System.out.println(driverNo);
        System.out.println("--------------------------------------");

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
     * 관리자: 전체 오더 목록 조회 (페이징 처리를 권장하지만, 일단 리스트로 구현)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
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
    
    public OrderResponse updateStatus(Long orderId, String newStatus, Long driverNo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 권한 검증: 이 오더를 배정받은 기사인지 확인
        if (order.getDriverNo() == null || !order.getDriverNo().equals(driverNo)) {
            throw new IllegalStateException("해당 주문의 담당 기사가 아닙니다.");
        }

        // 도메인 엔티티에 상태 변경 로직 위임
        order.changeStatus(newStatus);

        return OrderResponse.from(order);
    }
    
    /**
     * 드라이버 맞춤형 추천 오더 목록 조회
     */
    public List<OrderResponse> getRecommendedOrders(Long userId) {
        // 1. 드라이버 프로필 조회
        Driver driver = driverRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("드라이버 프로필을 찾을 수 없습니다."));

        // 2. 맞춤형 오더 조회
        // 주의: driver.getTonnage() 필드가 BigDecimal 타입인지 확인하세요.
        List<Order> recommendedOrders = orderRepository.findCustomOrders(
                driver.getCarType(), 
                driver.getTonnage() 
        );

        // 3. 응답 변환
        return recommendedOrders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }
    
    
    
    private OrderResponse convertToResponse(Order order) {
        // 1. Embedded 객체 추출
        OrderSnapshot snapshot = order.getSnapshot();
        
        // snapshot이 null일 경우를 대비한 방어 로직 (정상적인 주문이라면 null일 리 없지만 안전을 위해)
        if (snapshot == null) {
            throw new IllegalStateException("주문 상세 정보(Snapshot)가 존재하지 않습니다.");
        }

        return OrderResponse.builder()
                // 1. 주문 기본 정보 및 시스템 지표
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .distance(order.getDistance())
                .duration(order.getDuration())
                .createdAt(LocalDateTime.now())
                .updated(order.getUpdated())

                // 2. 상차지 정보 (Snapshot에서 추출)
                .startAddr(snapshot.getStartAddr())
                .startPlace(snapshot.getStartPlace())
                .startType(snapshot.getStartType())
                .startSchedule(snapshot.getStartSchedule())
                .puProvince(snapshot.getPuProvince())

                // 3. 하차지 정보 (Snapshot에서 추출)
                .endAddr(snapshot.getEndAddr())
                .endPlace(snapshot.getEndPlace())
                .endType(snapshot.getEndType())
                .endSchedule(snapshot.getEndSchedule())
                .doProvince(snapshot.getDoProvince())

                // 4. 화물 및 작업 정보 (Snapshot에서 추출)
                .cargoContent(snapshot.getCargoContent())
                .loadMethod(snapshot.getLoadMethod())
                .workType(snapshot.getWorkType())
                .tonnage(snapshot.getTonnage())
                .reqCarType(snapshot.getReqCarType())
                .reqTonnage(snapshot.getReqTonnage())
                .driveMode(snapshot.getDriveMode())
                .loadWeight(snapshot.getLoadWeight())

                // 5. 요금 정보 (Snapshot에서 추출)
                .basePrice(snapshot.getBasePrice())
                .laborFee(snapshot.getLaborFee())
                .packagingPrice(snapshot.getPackagingPrice())
                .insuranceFee(snapshot.getInsuranceFee())
                .payMethod(snapshot.getPayMethod())

                // 6. 연관 객체 요약 정보
                .cancellation(OrderResponse.CancellationSummary.from(order.getCancellationInfo()))
                .user(OrderResponse.UserSummary.from(order.getUser()))
                .build();
    }

    private final FareRepository farePolicyRepository;

    // 학원 레벨 기본값: 야간 22:00~06:00, 거리 km는 올림(ceiling)
    private static final int NIGHT_START_HOUR = 22;
    private static final int NIGHT_END_HOUR = 6;

    public long estimateFare(FareRequest req) {
        validate(req);

        LocalDateTime pickupAt = req.getPickupAt();
        long distanceMeters = req.getDistanceMeters();

        FarePolicy.DayType dayType = resolveDayType(pickupAt, req.getIsHoliday());
        FarePolicy.TimeType timeType = resolveTimeType(pickupAt);

        FarePolicy policy = farePolicyRepository
                .findTop1ByDayTypeAndTimeType(dayType, timeType)
                .orElseThrow(() -> new IllegalStateException("해당 요금표가 없습니다. dayType=" + dayType + ", timeType=" + timeType));

        int distanceKm = metersToChargedKm(distanceMeters); // 과금 km (올림)
        int extraKm = Math.max(0, distanceKm - policy.getBaseDistanceKm());

        long fare = policy.getBaseFare() + (policy.getPerKmFare() * extraKm);

        if (policy.getMinimumFare() != null) {
            fare = Math.max(fare, policy.getMinimumFare());
        }

        return fare;
    }

    private void validate(FareRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");
        if (req.getPickupAt() == null) throw new IllegalArgumentException("pickupAt is required");
        if (req.getDistanceMeters() == null || req.getDistanceMeters() < 0) {
            throw new IllegalArgumentException("distanceMeters is invalid");
        }
        if (req.getIsHoliday() == null) {
            req.setIsHoliday(false);
        }
    }

    private FarePolicy.DayType resolveDayType(LocalDateTime pickupAt, boolean isHoliday) {
        if (isHoliday) return FarePolicy.DayType.HOLIDAY;

        DayOfWeek dow = pickupAt.getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        return weekend ? FarePolicy.DayType.WEEKEND : FarePolicy.DayType.WEEKDAY;
    }

    private FarePolicy.TimeType resolveTimeType(LocalDateTime pickupAt) {
        int hour = pickupAt.getHour();

        boolean isNight = (hour >= NIGHT_START_HOUR) || (hour < NIGHT_END_HOUR);
        return isNight ? FarePolicy.TimeType.NIGHT : FarePolicy.TimeType.DAY;
    }

    // meters -> km 과금단위(올림): 0m=0km, 1m=1km, 1001m=2km
    private int metersToChargedKm(long meters) {
        if (meters == 0) return 0;
        return (int) ((meters + 999) / 1000);
    }

}