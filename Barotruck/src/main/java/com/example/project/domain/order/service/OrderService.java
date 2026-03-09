package com.example.project.domain.order.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.order.domain.AdminControl;
import com.example.project.domain.order.domain.CancellationInfo;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.domain.orderEnum.OrderDrivingStatus;
import com.example.project.domain.order.dto.MyRevenueResponse;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.global.neighborhood.NeighborhoodService;
import com.example.project.global.neighborhood.dto.NeighborhoodResponse;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.DriverRepository;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository; // 드라이버 정보 조회를 위한 레포지토리
    private final NeighborhoodService neighborhoodService;
    private final NotificationService notificationService; // [추가] 알림 서비스 주입
    private final TransportPaymentRepository transportPaymentRepository;
    private final TransportPaymentService transportPaymentService;
    private final UsersRepository usersRepository;

    /**
     * 1. 화주: 오더 생성 (C)
     */
    public OrderResponse createOrder(Users user, OrderRequest request) {
        // 주소 기반 지역 코드 자동 배정
        if (request.getStartAddr() != null) {
            try {
                NeighborhoodResponse nbh = neighborhoodService.resolveNeighborhood(request.getStartAddr());
                request.setStartNbhId(nbh.getNeighborhoodId());
            } catch (Exception e) {
                // 파싱 실패나 DB에 없는 지역일 경우 로그만 남기고 진행 (null로 저장됨)
                System.out.println("Start Address Neighborhood resolution failed: " + e.getMessage());
            }
        }
        if (request.getEndAddr() != null) {
            try {
                NeighborhoodResponse nbh = neighborhoodService.resolveNeighborhood(request.getEndAddr());
                request.setEndNbhId(nbh.getNeighborhoodId());
            } catch (Exception e) {
                System.out.println("End Address Neighborhood resolution failed: " + e.getMessage());
            }
        }

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

        if (order.getDriverList().contains(driverNo)) {
            throw new RuntimeException("이미 신청한 오더입니다.");
        }
        
        try {
            if (order.getSnapshot().isInstant()) {
                order.assignDriver(driverNo, "ACCEPTED");
                orderRepository.save(order);
                // [추가] 오더에 배차 완료 알림 발송 (화주에게)
                notificationService.sendNotification(
                        order.getUser(), // 화주(Shipper)
                        "ORDER",
                        "배차 완료",
                        String.format("기사님이 배차되었습니다. (차량번호: %s)",
                                driverRepository.findById(driverNo).map(Driver::getCarNum).orElse("정보 없음")),
                        order.getOrderId());
            } else {
                order.addToDriverList(driverNo);
                sendOrderNotificationSafely(
                        order.getUser(),
                        "배차 지원 도착",
                        String.format("새로운 기사님이 오더에 지원했습니다. (차량번호: %s)",
                                driverRepository.findById(driverNo).map(Driver::getCarNum).orElse("정보 없음")),
                        order.getOrderId());
            }
        }catch (ObjectOptimisticLockingFailureException e) {
            // 🚩 동시성 충돌 발생 시
            throw new RuntimeException("아쉽게도 방금 다른 차주님이 배차를 수락하셨습니다.");
        }
    }

    /**
     * 3. 차주: 수락 가능한 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders(Long userId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        List<Order> orders = orderRepository.findAvailableOrders("REQUESTED", now);
        
        System.out.println(orders);
        
        for(int i=0; i<orders.size(); i++) {
        	System.out.println(orders.indexOf(i));
        }

        return orders.stream()
        		.map(order -> convertToResponse(order, userId))
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
        Users driver = usersRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("차주를 찾을 수 없습니다."));
        if (driver.isAdminForceAllocateBlocked()) {
            throw new IllegalStateException("해당 차주는 관리자 강제배차를 허용하지 않았습니다.");
        }

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
    /*
     * public void cancelOrder(Long orderId, String cancelReason, Users currentUser)
     * {
     * Order order = orderRepository.findById(orderId)
     * .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));
     * 
     * String role = determineCancelRole(order, currentUser); // 취소 주체 판별
     * 
     * // 1. 상태별 취소 가능 여부 체크
     * validateCancelCondition(order, role);
     * 
     * // 2. 오더 상태 업데이트
     * String newStatus = "CANCELLED_BY_" + role;
     * order.cancelOrder(newStatus); //
     * 
     * // 3. 취소 정보 생성 및 연관관계 설정
     * CancellationInfo cancelInfo = CancellationInfo.builder()
     * .order(order)
     * .cancelReason(cancelReason)
     * .cancelledAt(LocalDateTime.now())
     * .cancelledBy(role)
     * .build();
     * 
     * order.setCancellationInfo(cancelInfo); //
     * orderRepository.save(order);
     * }
     */

    // 유림 수정
    public void cancelOrder(Long orderId, String cancelReason, Users currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        String role = determineCancelRole(order, currentUser);

        // 1. 상태별 취소 가능 여부 체크
        validateCancelCondition(order, role);

        // 2. 역할에 따른 취소 처리 방식 분리
        if ("APPLICANT".equals(role)) {
            // 승인 대기 상태에서 취소: 지원자 명단에서만 내 이름 지우기
            order.getDriverList().remove(currentUser.getUserId());
            orderRepository.save(order);
            return; // 오더 자체가 취소되는 것은 아니므로 여기서 메서드 종료
            
        } else if ("DRIVER".equals(role)) {
            // 이미 확정된 기사가 취소: 오더를 다시 REQUESTED로 초기화
            order.assignDriver(null, "REQUESTED");
        } else {
            // 화주나 관리자가 취소한 경우: 아예 종료 상태로 변경
            String newStatus = "CANCELLED_BY_" + role;
            order.cancelOrder(newStatus);
        }

        // 3. 취소 정보 생성 (기존 로직 유지 - 누가 왜 취소했는지 기록은 남겨야 함)
        CancellationInfo cancelInfo = CancellationInfo.builder()
                .order(order)
                .cancelReason(cancelReason)
                .cancelledAt(LocalDateTime.now())
                .cancelledBy(role)
                .build();

        order.setCancellationInfo(cancelInfo);
        orderRepository.save(order);

        if ("DRIVER".equals(role)) {
            sendOrderNotificationSafely(
                    order.getUser(),
                    "차주 배차 취소",
                    "배정된 차주가 주문을 취소했습니다.",
                    order.getOrderId());
        } else if ("USER".equals(role) && order.getDriverNo() != null) {
            sendOrderNotificationSafely(
                    order.getDriverNo(),
                    "주문 취소",
                    "화주가 주문을 취소했습니다.",
                    order.getOrderId());
        }
    }

    private String determineCancelRole(Order order, Users user) {
        if (user.getRole().name().equals("ADMIN"))
            return "ADMIN";
        if (order.getUser().getUserId().equals(user.getUserId()))
            return "USER";
        if (order.getDriverNo() != null && order.getDriverNo().equals(user.getUserId()))
            return "DRIVER";
        
        // 배차 신청만 한 상태
        if (order.getDriverList().contains(user.getUserId()))
            return "APPLICANT";

        
        throw new RuntimeException("취소 권한이 없습니다.");
    }

    private void validateCancelCondition(Order order, String role) {
        if (role.equals("ADMIN"))
            return; // 관리자는 무조건 가능

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
        System.out.println("=== 상태 변경 시작 ===");
        System.out.println("입력 데이터 -> orderId: " + orderId + ", newStatus: " + newStatus + ", driverNo: " + driverNo);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        System.out.println("DB 조회 결과 -> order.getDriverNo(): " + order.getDriverNo());

        // [중요 체크] 여기서 튕길 가능성이 매우 높습니다.
        if (order.getDriverNo() == null || !order.getDriverNo().equals(driverNo)) {
            System.out.println("!!! 권한 검증 실패: 담당 기사가 아님 !!!");
            throw new IllegalStateException("해당 주문의 담당 기사가 아닙니다.");
        }
        System.out.println("권한 검증 통과");

        String normalizedStatus = normalizeDrivingStatus(newStatus);
        String statusMessage = convertStatusToMessage(normalizedStatus);
        System.out.println("상태 변환 결과: " + normalizedStatus + " (" + statusMessage + ")");

        try {
            notificationService.sendNotification(
                    order.getUser(),
                    "ORDER",
                    "주문 상태 변경",
                    "주문 상태가 '" + statusMessage + "'(으)로 변경되었습니다.",
                    order.getOrderId());
            System.out.println("알림 발송 완료");
        } catch (Exception e) {
            System.out.println("알림 발송 중 예외 발생: " + e.getMessage());
        }

        System.out.println("엔티티 상태 변경 전: " + order.getStatus());
        order.changeStatus(normalizedStatus);
        System.out.println("엔티티 상태 변경 후: " + order.getStatus());

        Order savedOrder = orderRepository.save(order);

        if ("COMPLETED".equals(normalizedStatus)) {
            try {
                transportPaymentService.ensureReadyPaymentRecord(orderId);
            } catch (Exception e) {
                System.err.println("결제 READY 레코드 생성 실패: " + e.getMessage());
            }
        }

        // save 후 즉시 반영을 확인하고 싶다면 아래 주석 해제 (단, 로그 확인용)
        // orderRepository.flush(); 
        
        System.out.println("Repository save 완료 (ID: " + savedOrder.getOrderId() + ")");
        System.out.println("=== 상태 변경 종료 ===");
        
     // 2. 에러가 숨어있는 곳을 잡기 위한 Try-Catch 덫
        try {
            return convertToResponse(order);
        } catch (Exception e) {
            System.err.println("!!! DTO 변환 중 치명적 에러 발생 !!! : " + e.getMessage());
            e.printStackTrace(); // 어디서 터졌는지 상세 줄 번호를 출력합니다.
            throw e; // 에러를 다시 던져서 프론트에 오류를 알림
        }
    }

    // (참고) 상태 메시지 변환 헬퍼
    private String convertStatusToMessage(String status) {
        switch (status) {
            case "LOADING":
                return "상차 중";
            case "MOVING":
                return "이동 중";
            case "COMPLETE":
                return "배송 완료";
            default:
                return status;
        }
    }

    private String normalizeDrivingStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (status.isEmpty()) {
            throw new IllegalArgumentException("newStatus is required");
        }
        if ("MOVING".equals(status)) {
            status = "IN_TRANSIT";
        } else if ("COMPLETE".equals(status)) {
            status = "COMPLETED";
        }
        if (!OrderDrivingStatus.asStrings().contains(status)) {
            throw new IllegalArgumentException("unsupported order status: " + rawStatus);
        }
        return status;
    }

    public List<OrderResponse> findMyDrivingOrders(Long driverId) {
        // 운행 중으로 간주되는 상태 리스트 정의
        List<String> drivingStatuses = List.of(
                "REQUESTED", // 배차대기
                "APPLIED", // 승인 대기
                "ACCEPTED", // 배차확정
                "LOADING", // 상차중
                "IN_TRANSIT", // 이동중
                "UNLOADING", // 하차중
                "COMPLETED" // 하차완료
        );

        // 리포지토리를 통해 해당 차주 ID와 상태 목록에 해당하는 오더 조회
        return orderRepository.findMyDrivingAndAppliedOrders(driverId, drivingStatuses)
                .stream()
                .map(order -> convertToResponse(order, driverId)) // 엔티티 -> DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 드라이버 맞춤형 추천 오더 목록 조회
     */
    public List<OrderResponse> getRecommendedOrders(Long userId) {
        // 1. 드라이버 프로필 조회
        Driver driver = driverRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("드라이버 프로필을 찾을 수 없습니다."));

        // 차주의 선호 지역 코드 사용 (없으면 null -> 전체 지역 조회)
        Long nbhId = driver.getNbhId();

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // 2. 맞춤형 오더 조회
        // 주의: driver.getTonnage() 필드가 BigDecimal 타입인지 확인하세요.
        List<Order> recommendedOrders = orderRepository.findCustomOrders(
                driver.getCarType(),
                driver.getTonnage(),
                now);

        // 3. 응답 변환
        return recommendedOrders.stream()
        		.map(order -> convertToResponse(order, userId))
                .collect(Collectors.toList());
    }

    /**
     * 5. 차주: 지역 기반 오더 검색 (지역코드 OR 주소)
     * nbhId가 있으면 우선 사용, 없으면 address를 파싱하여 지역 코드 추출 후 검색
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(Users user, Long nbhId, String address) {
        Driver driver = driverRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("드라이버 프로필을 찾을 수 없습니다."));
        System.out.println(driver);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        
        Long targetNbhId = nbhId;

        // 지역코드가 없고 주소가 있다면 주소를 통해 지역코드 추출
        if (targetNbhId == null && address != null && !address.isBlank()) {
            try {
                NeighborhoodResponse nbh = neighborhoodService.resolveNeighborhood(address);
                targetNbhId = nbh.getNeighborhoodId();
            } catch (Exception e) {
                // 주소 파싱 실패 시 검색 결과 없음 처리 (빈 리스트 반환)
                return new ArrayList<>();
            }
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        // 기존의 맞춤형 오더 조회 쿼리 재사용 (차종/톤수 필터 + 지역 필터)
        return orderRepository.findCustomOrders(driver.getCarType(), driver.getTonnage(), now)
                .stream()
                .map(order -> convertToResponse(order, user.getUserId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void selectDriver(Long orderId, Long selectedDriverNo, Long currentUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 오더가 존재하지 않습니다."));

        // 1. 보안 체크: 요청자가 해당 오더의 주인(화주)인지 확인
        if (!(order.getUser().getUserId() == currentUserId)) {
            throw new RuntimeException("해당 오더를 관리할 권한이 없습니다.");
        }

        // 2. 상태 체크: 공백 제거 및 대소문자 무시 비교로 변경
        String currentStatus = (order.getStatus() != null) ? order.getStatus().trim() : "";

        if (!("REQUESTED".equalsIgnoreCase(currentStatus) || ("APPLIED".equalsIgnoreCase(currentStatus)))) {
            System.out.println("실제 상태값 확인: [" + order.getStatus() + "]"); // 디버깅용
            throw new RuntimeException("이미 배차가 완료되었거나 취소된 오더입니다. 현재 상태: " + order.getStatus());
        }

        // 3. 선택한 기사가 신청자 명단에 있는지 확인 및 최종 확정
        // 아까 Order 엔티티에 만들었던 confirmDriver 편의 메서드 활용
        order.confirmDriver(selectedDriverNo);

        // 4. 선택되지 않은 나머지 신청자 명단은 비워주기 (선택 사항)
        order.getDriverList().clear();

        sendOrderNotificationSafely(
                selectedDriverNo,
                "배차 확정",
                "화주가 회원님을 최종 기사로 선택했습니다.",
                order.getOrderId());
    }

    /**
     * 화주 전용: 본인이 생성한 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> findMyShipperOrders(Long userId) {
        // 리포지토리에서 화주 ID로 조회 (최신순 정렬은 레포지토리 메서드 명으로 처리)
        return orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> convertToResponse(order, userId))
                .collect(Collectors.toList());
    }

    // OrderService.java에 추가

    public MyRevenueResponse getMyMonthlyRevenue(Long driverNo, Integer year, Integer month) {
        // 1. 기간 설정
        LocalDateTime now = LocalDateTime.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        LocalDateTime start = LocalDateTime.of(targetYear, targetMonth, 1, 0, 0, 0);
        LocalDateTime end = start.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        // 2. 수익 통계 조회 (List<Object[]>로 받아 첫 행 추출)
        List<Object[]> results = orderRepository.findRevenueStatsByDriver(driverNo, start, end);
        System.out.println("-----------------------");
        System.out.println(results);
        System.out.println("-----------------------");

        long total = 0, received = 0, pending = 0;

        if (results != null && !results.isEmpty()) {
            Object[] stats = results.get(0);
            total = (stats[0] != null) ? ((Number) stats[0]).longValue() : 0L;
            received = (stats[1] != null) ? ((Number) stats[1]).longValue() : 0L;
            pending = (stats[2] != null) ? ((Number) stats[2]).longValue() : 0L;
            System.out.println("통계 결과 -> 총액: " + total + ", 수령: " + received + ", 예정: " + pending);
        }

        // 3. 해당 월 오더 목록 조회 및 변환
        List<OrderResponse> orderList = orderRepository.findMonthlyOrdersByDriver(driverNo, start, end)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return MyRevenueResponse.builder()
                .totalAmount(total)
                .receivedAmount(received)
                .pendingAmount(pending)
                .orders(orderList)
                .build();
    }
    
    @Transactional
    public void cancelExpiredOrders() {
        // 1. 현재 시간 기준으로 24시간 전 시간 계산
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        
        // 2. 취소 대상 조회: 상태가 'REQUESTED'이고 상차 예정일이 24시간 이상 지난 오더들
        // (startSchedule이 String 형식이므로 DB 함수를 사용하여 비교하거나 로직상 처리가 필요합니다)
        List<Order> expiredOrders = orderRepository.findAll().stream()
                .filter(o -> "REQUESTED".equals(o.getStatus()))
                .filter(o -> {
                    try {
                        // String 타입의 startSchedule을 LocalDateTime으로 변환 (패턴은 프로젝트 설정에 맞춤)
                        LocalDateTime schedule = LocalDateTime.parse(o.getSnapshot().getStartSchedule(), 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        return schedule.isBefore(threshold);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // 3. 대상 오더들 취소 처리
        for (Order order : expiredOrders) {
            order.changeStatus("CANCELLED");
            
            // CancellationInfo 생성 및 저장
            CancellationInfo cancelInfo = CancellationInfo.builder()
                    .order(order)
                    .cancelReason("미배정으로 인한 시스템 자동 취소")
                    .cancelledAt(LocalDateTime.now())
                    .cancelledBy("SYSTEM")
                    .build();
            
            // 프로젝트 구조에 따라 cancelInfoRepository.save(cancelInfo) 호출 필요
            order.setCancellationInfo(cancelInfo); 
        }
    }
    
    

    private OrderResponse convertToResponse(Order order) {
        // 1. Embedded 객체 추출
        OrderSnapshot snapshot = order.getSnapshot();

        // snapshot이 null일 경우를 대비한 방어 로직 (정상적인 주문이라면 null일 리 없지만 안전을 위해)
        if (snapshot == null) {
            throw new IllegalStateException("주문 상세 정보(Snapshot)가 존재하지 않습니다.");
        }
        // tag 리스트를 안전하게 복사하여 주입
        List<String> tags = snapshot.getTag() != null ? new ArrayList<>(snapshot.getTag()) : new ArrayList<>();
        List<Long> driverIds = new ArrayList<>(order.getDriverList());

        return OrderResponse.builder()
                // 1. 주문 기본 정보 및 시스템 지표
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .settlementStatus(resolveSettlementStatus(order))
                .distance(order.getDistance())
                .duration(order.getDuration())
                .createdAt(order.getCreatedAt())
                .updated(order.getUpdated())
                .driverNo(order.getDriverNo())
                .driverList(driverIds)

                .startLat(snapshot.getStartLat())
                .startLng(snapshot.getStartLng())
                // 2. 상차지 정보 (Snapshot에서 추출)
                .startAddr(snapshot.getStartAddr())
                .startPlace(snapshot.getStartPlace())
                .startType(snapshot.getStartType())
                .startSchedule(snapshot.getStartSchedule())
                .puProvince(snapshot.getPuProvince())
                .startNbhId(snapshot.getStartNbhId())

                // 3. 하차지 정보 (Snapshot에서 추출)
                .endAddr(snapshot.getEndAddr())
                .endPlace(snapshot.getEndPlace())
                .endType(snapshot.getEndType())
                .endSchedule(snapshot.getEndSchedule())
                .doProvince(snapshot.getDoProvince())
                .endNbhId(snapshot.getEndNbhId())

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
                .instant(snapshot.isInstant())
                .memo(snapshot.getMemo())
                .tag(tags)

                // 6. 연관 객체 요약 정보
                .cancellation(OrderResponse.CancellationSummary.from(order.getCancellationInfo()))
                .user(OrderResponse.UserSummary.from(order.getUser()))
                .build();
    }
    
 // OrderService.java 내부에 있는 메서드 교체

    private String resolveSettlementStatus(Order order) {
        if (order == null || order.getOrderId() == null) {
            return "READY";
        }

        try {
            // 결제 정보 조회 시도 (여기서 터지더라도 catch가 잡아줍니다)
            return transportPaymentRepository.findByOrderId(order.getOrderId())
                    .map(payment -> payment.getStatus() != null ? payment.getStatus().name() : "READY")
                    .orElseGet(() -> {
                        if (order.getSettlement() != null && order.getSettlement().getStatus() != null) {
                            return order.getSettlement().getStatus().name();
                        }
                        return "READY";
                    });
        } catch (Exception e) {
            // 🚨 핵심 해결책: 에러가 발생하면 로깅만 하고 기본값인 "READY"를 반환하여 롤백을 방지합니다.
            System.err.println("결제 상태 조회 중 에러 발생 (무시하고 READY 반환): " + e.getMessage());
            return "READY";
        }
    }

    private OrderResponse convertToResponse(Order order, Long currentUserId) {
        // 1. 지연 로딩 방지: 지원자 명단을 확실하게 불러옵니다.
        if (order.getDriverList() != null) {
            order.getDriverList().size();
        }

        // 2. 기존 팀원이 만든 메서드를 호출하여 기본 정보를 세팅합니다.
        OrderResponse res = convertToResponse(order);
        if (res == null) return null;

        // 3. 만약 내가 신청한 오더라면 상태를 APPLIED로 바꿉니다.
        if ("REQUESTED".equals(res.getStatus()) && currentUserId != null && order.getDriverList().contains(currentUserId)) {
            res.setStatus("APPLIED");
        }
        return res;
    }

    private void sendOrderNotificationSafely(Users recipient, String title, String body, Long orderId) {
        if (recipient == null) {
            return;
        }
        try {
            notificationService.sendNotification(recipient, "ORDER", title, body, orderId);
        } catch (Exception e) {
            System.out.println("오더 알림 발송 중 예외 발생: " + e.getMessage());
        }
    }

    private void sendOrderNotificationSafely(Long driverNo, String title, String body, Long orderId) {
        if (driverNo == null) {
            return;
        }
        try {
            notificationService.sendNotification(driverNo, "ORDER", title, body, orderId);
        } catch (Exception e) {
            System.out.println("오더 알림 발송 중 예외 발생: " + e.getMessage());
        }
    }
    
 // OrderService.java

    @Transactional
    public OrderResponse updateOrder(Long orderId, Users user, OrderRequest request) {
        // 1. 오더 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("오더를 찾을 수 없습니다."));

        // 2. 본인 오더인지 검증
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인이 등록한 오더만 수정할 수 있습니다.");
        }

        // 3. 수정 가능 상태 확인 (배차 대기 중인 경우만 수정 가능)
        if (!"REQUESTED".equals(order.getStatus())) {
            throw new IllegalStateException("이미 배차가 완료되었거나 취소된 오더는 수정할 수 없습니다.");
        }

        Order patchOrder = Order.createOrder(user, request);
        Order savedOrder = orderRepository.save(patchOrder);
        return convertToResponse(savedOrder);
        
    }

}
