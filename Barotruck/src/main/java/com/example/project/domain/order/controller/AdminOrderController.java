package com.example.project.domain.order.controller;

import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.service.OrderService;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // 모든 오더 조회
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    // 강제 배차
    @PatchMapping("/{orderId}/force-allocate")
    public ResponseEntity<String> forceAllocate(
            @PathVariable Long orderId,
            @RequestParam Long driverId,
            @RequestParam String reason,
            @AuthenticationPrincipal Users admin) {
        orderService.forceAllocateOrder(orderId, driverId, admin.getEmail(), reason);
        return ResponseEntity.ok("강제 배차가 완료되었습니다.");
    }

    // 관리자 권한 취소
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrderByAdmin(
            @PathVariable Long orderId,
            @RequestParam String reason,
            @AuthenticationPrincipal Users admin) {
        orderService.adminCancelOrder(orderId, admin.getEmail(), reason);
        return ResponseEntity.ok("관리자에 의해 오더가 취소되었습니다.");
    }
    
 // 취소된 오더 목록 조회 API
    @GetMapping("/cancelled")
    public ResponseEntity<List<OrderResponse>> getCancelledOrders() {
        return ResponseEntity.ok(orderService.getCancelledOrdersForAdmin());
    }
    
 // AdminOrderController.java 내부에 추가
    // 관리자가 모든 오더를 모니터링하다가 강제로 취소할 때 사용합니다.
    @DeleteMapping("/{orderId}/force-cancel")
    public ResponseEntity<String> forceCancel(
            @PathVariable Long orderId,
            @RequestParam String reason,
            @AuthenticationPrincipal Users admin) {
        // 관리자 로직도 동일한 서비스를 호출하되, 서비스 내부에서 ADMIN 권한으로 처리됨
        orderService.cancelOrder(orderId, reason, admin);
        return ResponseEntity.ok("관리자 권한으로 오더를 강제 취소했습니다.");
    }
    
}