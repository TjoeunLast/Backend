package com.example.project.domain.order.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.order.domain.Order.ProvinceStatResponse;
import com.example.project.domain.order.domain.Order.RouteStatisticsResponse;
import com.example.project.domain.order.dto.OrderResponse;
import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.domain.order.service.AdminOrderService;
import com.example.project.global.api.PaginationUtils;
import com.example.project.domain.order.service.orderService.OrderDriverQueryService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService orderService;
    private final OrderDriverQueryService orderDriverQueryService;

    // 모든 오더 조회
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable,
            NativeWebRequest webRequest) {
        List<OrderResponse> orders = orderService.getAllOrdersForAdmin();
        if (!PaginationUtils.isPagedRequest(webRequest)) {
            return ResponseEntity.ok(orders);
        }
        return ResponseEntity.ok(PaginationUtils.paginate(orders, pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users admin) {
        return ResponseEntity.ok(orderService.getOrderDetailForAdmin(orderId, admin));
    }

    @GetMapping("/{orderId}/applicants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AssignedDriverInfoResponse>> getApplicants(
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderDriverQueryService.getApplicantsInfo(orderId));
    }

    // 강제 배차
    @PatchMapping("/{orderId}/force-allocate")
    public ResponseEntity<String> forceAllocate(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @RequestParam("driverID") Long driverId,
            @AuthenticationPrincipal Users admin) {
        orderService.forceAllocateOrder(orderId, driverId, admin.getEmail(), reason);
        return ResponseEntity.ok("강제 배차가 완료되었습니다.");
    }

    // 관리자 권한 취소
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrderByAdmin(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal Users admin) {
        orderService.adminCancelOrder(orderId, admin.getEmail(), reason);
        return ResponseEntity.ok("관리자에 의해 오더가 취소되었습니다.");
    }

    // 취소된 오더 목록 조회 API
    @GetMapping("/cancelled")
    public ResponseEntity<?> getCancelledOrders(
            @PageableDefault(size = 20) Pageable pageable,
            NativeWebRequest webRequest) {
        List<OrderResponse> orders = orderService.getCancelledOrdersForAdmin();
        if (!PaginationUtils.isPagedRequest(webRequest)) {
            return ResponseEntity.ok(orders);
        }
        return ResponseEntity.ok(PaginationUtils.paginate(orders, pageable));
    }

    // 강제 취소 (DeleteMapping "/force-cancel")
    @DeleteMapping("/{orderId}/force-cancel")
    public ResponseEntity<String> forceCancel(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal Users admin) {
        // 정의되지 않았던 cancelOrder 대신 adminCancelOrder를 호출하도록 수정
        orderService.adminCancelOrder(orderId, admin.getEmail(), reason);
        return ResponseEntity.ok("관리자 권한으로 오더를 강제 취소했습니다.");
    }

    @GetMapping("/provinces")
    public ResponseEntity<Map<String, List<ProvinceStatResponse>>> getProvinceStats(
            @RequestParam(name = "period", defaultValue = "month") String period) {
        // 만약 전체 기간 통계가 필요하다면 Service에서 period가 "all"일 때 처리하도록 로직을 짜는 것이 좋습니다.
        return ResponseEntity.ok(orderService.getProvinceStatsByPeriod(period));
    }

    /**
     * 노선별(출발-도착 쌍) 물동량 통계 조회
     */
    @GetMapping("/statistics/routes")
    public ResponseEntity<List<RouteStatisticsResponse>> getRouteStats() {
        return ResponseEntity.ok(orderService.getRouteStatistics());
    }



    /**
     * 관리자 종합 통계 조회 (노선별 건수, 지역별 건수/매출)
     * @param period : day, week, month, year (기본값 day)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAdminStats(
            @RequestParam(name="period",defaultValue = "month") String period) {
        return ResponseEntity.ok(orderService.getComprehensiveStats(period));
    }

}
