package com.example.project.domain.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // DTO 임포트
import com.example.project.domain.order.service.OrderService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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

    // 차주: 오더 수락 (배차 신청)
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<String> accept(
            @PathVariable("orderId") Long orderId, 
            @AuthenticationPrincipal Users user) {
        orderService.acceptOrder(orderId, user.getDriver().getDriverId());
        return ResponseEntity.ok("배차가 성공적으로 완료되었습니다.");
    }
    
 // OrderController.java 내부에 추가
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancel(
            @PathVariable Long orderId,
            @RequestParam String reason,
            @AuthenticationPrincipal Users user) {
        orderService.cancelOrder(orderId, reason, user);
        return ResponseEntity.ok("오더 취소가 완료되었습니다.");
    }
    
    // 차주가 상태 변경하는 함수 운행중 운행완료 기타 등등...
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String newStatus,
            @AuthenticationPrincipal Users userDetails) {
        
        // 현재 로그인한 사용자가 드라이버인지 권한 체크가 필요할 수 있습니다.
        OrderResponse response = orderService.updateStatus(orderId, newStatus, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
    
}