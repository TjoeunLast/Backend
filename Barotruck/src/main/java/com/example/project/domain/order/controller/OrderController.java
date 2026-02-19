package com.example.project.domain.order.controller;

import java.util.List;

import com.example.project.domain.order.service.orderService.OrderQueryService;
import org.springframework.data.domain.Page;
import com.example.project.domain.order.dto.orderRequest.OrderSearchRequest;
import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.dto.orderResponse.OrderListResponse;
import com.example.project.domain.order.service.orderService.FareService;
import com.example.project.domain.order.service.orderService.OrderDriverQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // DTO 임포트
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.domain.order.service.OrderService;
import com.example.project.domain.order.service.orderService.FareService;
import com.example.project.domain.order.service.orderService.OrderDriverQueryService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final FareService fareService;
    private final OrderDriverQueryService orderDriverQueryService;
    private final OrderQueryService orderQueryService;


    // 화주: 신규 오더 요청 (반환 타입을 OrderResponse로 변경)
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal Users user, 
            @RequestBody OrderRequest request) {
        // 서비스에서 변환까지 마친 DTO를 받아서 반환합니다.
        return ResponseEntity.ok(orderService.createOrder(user, request));
    }

    // 차주: 배차 대기 중인 오더 목록 조회 (반환 타입을 List<OrderResponse>로 변경)
    @GetMapping("/available")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    /**
     * 신규: 드라이버의 차량 사격에 맞춘 '추천 오더' 조회
     * 인증된 사용자의 정보를 기반으로 내 차에 맞는 짐만 필터링합니다.
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<OrderResponse>> getRecommendedOrders(
            @AuthenticationPrincipal Users user) {
        // 로그인된 사용자의 ID를 넘겨 서비스에서 드라이버 정보를 찾음
        List<OrderResponse> responses = orderService.getRecommendedOrders(user.getUserId());
        return ResponseEntity.ok(responses);
    }
    
    // 차주: 오더 수락 (배차 신청)
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<String> accept(
            @PathVariable("orderId") Long orderId, 
            @AuthenticationPrincipal Users user) {
        orderService.acceptOrder(orderId, user.getUserId());
        return ResponseEntity.ok("배차가 성공적으로 완료되었습니다.");
    }
    
 // OrderController.java 내부에 추가
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancel(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal Users user) {
        orderService.cancelOrder(orderId, reason, user);
        return ResponseEntity.ok("오더 취소가 완료되었습니다.");
    }
    
    // 차주가 상태 변경하는 함수 운행중 운행완료 기타 등등...
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("newStatus") String newStatus,
            @AuthenticationPrincipal Users userDetails) {
        
        // 현재 로그인한 사용자가 드라이버인지 권한 체크가 필요할 수 있습니다.
        OrderResponse response = orderService.updateStatus(orderId, newStatus, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 차주 전용: 현재 내가 배차받아 운행 중인 오더 목록 조회
     * 대상 상태: ACCEPTED, LOADING, IN_TRANSIT, UNLOADING
     */
    @GetMapping("/my-driving")
    public ResponseEntity<List<OrderResponse>> getMyDrivingOrders(@AuthenticationPrincipal Users user) {
        // 현재 로그인한 사용자의 ID와 특정 상태값들을 서비스로 전달
        List<OrderResponse> orders = orderService.findMyDrivingOrders(user.getUserId());
        return ResponseEntity.ok(orders);
    }
    

    @PostMapping("/fare")
    public ResponseEntity<Long> estimateFare(@RequestBody FareRequest request) {
        long fare = fareService.estimateFare(request);
        return ResponseEntity.ok(fare);
    }


    // 배정된 차주 정보 확인 (조회 전용)
    @GetMapping("/{driverNo}/assigned-info")
    public ResponseEntity<AssignedDriverInfoResponse> getAssignedDriver(
            @PathVariable("driverNo") Long driverNo
    ) {
        return ResponseEntity.ok(orderDriverQueryService.getAssignedDriverInfo(driverNo));
    }
    
    
    /**
     * 1. 특정 오더에 신청한 차주 리스트 조회 API
     * GET /api/v1/orders/{orderId}/applicants
     */
    @GetMapping("/{orderId}/applicants")
    public ResponseEntity<List<AssignedDriverInfoResponse>> getApplicants(
            @PathVariable("orderId") Long orderId) {
        List<AssignedDriverInfoResponse> applicants = orderDriverQueryService.getApplicantsInfo(orderId);
        return ResponseEntity.ok(applicants);
    }

    /**
     * 2. 화주가 차주를 최종 선택(배차 확정)하는 API
     * POST /api/v1/orders/{orderId}/select-driver
     */
    @PostMapping("/{orderId}/select-driver")
    public ResponseEntity<String> selectDriver(
            @PathVariable("orderId") Long orderId,
            @RequestParam("driverNo") Long driverNo, // 선택된 차주의 ID
            @AuthenticationPrincipal Users user) { // 로그인한 화주의 ID
        
        orderService.selectDriver(orderId, driverNo, user.getUserId());
        return ResponseEntity.ok("배차가 성공적으로 확정되었습니다.");
    }

    // 예: /api/orders?tab=RECOMMEND
    //                &puProvince=서울
    //                &minReqTonnage=5
    //                &reqCarType=윙바디
    //                &sort=RECOMMEND&page=0
    //                &size=20
    @GetMapping
    public Page<OrderListResponse> list(@ModelAttribute OrderSearchRequest request) {
        return orderQueryService.search(request);
    }
    
    
    /**
     * 화주 전용: 내가 등록한 모든 오더 목록 조회
     * (최신 등록순)
     */
    @GetMapping("/my-shipper")
    public ResponseEntity<List<OrderResponse>> getMyShipperOrders(@AuthenticationPrincipal Users user) {
        // 화주 권한 체크 (선택 사항이나 권장)
        if (!user.getRole().name().equals("SHIPPER")) {
            throw new IllegalStateException("화주 권한이 필요한 서비스입니다.");
        }
        
        List<OrderResponse> orders = orderService.findMyShipperOrders(user.getUserId());
        return ResponseEntity.ok(orders);
    }
    

}