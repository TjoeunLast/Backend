package com.example.project.domain.order.controller;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.service.OrderService;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 화주: 신규 오더 요청
    @PostMapping
    public ResponseEntity<Order> create(@AuthenticationPrincipal Users user, @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(user, request));
    }

    // 차주: 배차 대기 중인 오더 목록 조회
    @GetMapping("/available")
    public ResponseEntity<List<Order>> getAvailableOrders() {
        // 서비스에서 findByStatus("REQUESTED") 호출
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    // 차주: 오더 수락 (배차 신청)
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<String> accept(@PathVariable Long orderId, @RequestParam Long driverNo) {
        orderService.acceptOrder(orderId, driverNo);
        return ResponseEntity.ok("배차가 성공적으로 완료되었습니다.");
    }
}