package com.example.project.domain.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.service.AdminOrderService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService orderService;

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
    
 // 강제 취소 (DeleteMapping "/force-cancel")
    @DeleteMapping("/{orderId}/force-cancel")
    public ResponseEntity<String> forceCancel(
            @PathVariable Long orderId,
            @RequestParam String reason,
            @AuthenticationPrincipal Users admin) {
        // 정의되지 않았던 cancelOrder 대신 adminCancelOrder를 호출하도록 수정
    	orderService.adminCancelOrder(orderId, admin.getEmail(), reason);
        return ResponseEntity.ok("관리자 권한으로 오더를 강제 취소했습니다.");
    }
    
}