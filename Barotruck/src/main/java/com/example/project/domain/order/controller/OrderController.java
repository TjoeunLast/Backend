package com.example.project.domain.order.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.domain.order.dto.MyRevenueResponse;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.dto.OrderResponse; // DTO 임포트
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.dto.orderRequest.OrderSearchRequest;
import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.domain.order.dto.orderResponse.OrderListResponse;
import com.example.project.domain.order.service.OrderService;
import com.example.project.domain.order.service.orderService.FareService;
import com.example.project.domain.order.service.orderService.OrderDriverQueryService;
import com.example.project.domain.order.service.orderService.OrderQueryService;
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
    public ResponseEntity<List<OrderResponse>> getAvailableOrders(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(orderService.getAvailableOrders(user.getUserId()));
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
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("newStatus") String newStatus,
            @AuthenticationPrincipal Users userDetails) {

        // 현재 로그인한 사용자가 드라이버인지 권한 체크가 필요할 수 있습니다.
        orderService.updateStatus(orderId, newStatus, userDetails.getUserId());
        return ResponseEntity.ok("정상처리");
    }

    /**
     * 차주 전용: 현재 내가 배차받아 운행 중인 오더 목록 조회
     * 대상 상태: ACCEPTED, LOADING, IN_TRANSIT, UNLOADING
     */
    @GetMapping("/my-driving")
    @PreAuthorize("hasRole('DRIVER')")
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
            @PathVariable("driverNo") Long driverNo) {
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
    // &puProvince=서울
    // &minReqTonnage=5
    // &reqCarType=윙바디
    // &sort=RECOMMEND&page=0
    // &size=20
    @GetMapping
    public Page<OrderListResponse> list(@ModelAttribute OrderSearchRequest request) {
        return orderQueryService.search(request);
    }

    /**
     * 화주 전용: 내가 등록한 모든 오더 목록 조회
     * (최신 등록순)
     */
    @GetMapping("/my-shipper")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<List<OrderResponse>> getMyShipperOrders(@AuthenticationPrincipal Users user) {
        List<OrderResponse> orders = orderService.findMyShipperOrders(user.getUserId());
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/fare")
    public long estimateAndSaveFare(
            @PathVariable Long orderId,
            @RequestBody FareRequest req) {
        return fareService.estimateAndSaveFare(orderId, req);
    }

    @GetMapping("/my-revenue")
    public ResponseEntity<MyRevenueResponse> getMyRevenue(
            @AuthenticationPrincipal Users user,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {

        // 연/월 정보가 없으면 현재 달로 처리하도록 서비스 호출
        MyRevenueResponse response = orderService.getMyMonthlyRevenue(user.getUserId(), year, month);
        return ResponseEntity.ok(response);
    }

    // 5. 차주: 지역 기반 오더 검색
    // GET /api/v1/orders/search?nbhId=11110 OR /api/v1/orders/search?address=서울 종로구
    @GetMapping("/search")
    public ResponseEntity<List<OrderResponse>> searchOrders(
            @AuthenticationPrincipal Users user,
            @RequestParam(required = false) Long nbhId,
            @RequestParam(required = false) String address) {

        // 서비스의 searchOrders 메서드 호출
        return ResponseEntity.ok(orderService.searchOrders(user, nbhId, address));
    }

    // OrderController.java

    @PutMapping("/{orderId}")
    public ResponseEntity<String> updateOrder(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users user,
            @RequestBody OrderRequest request) {
        orderService.updateOrder(orderId, user, request);
        return ResponseEntity.ok("오더 정보가 성공적으로 수정되었습니다.");
    }

    /**
     * 오더 이미지 등록 및 수정
     * POST /api/v1/orders/{orderId}/image
     * form-data key: "image"
     */
    @PostMapping(value = "/{orderId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadOrderImage(
            @PathVariable("orderId") Long orderId,
            @RequestParam("image") MultipartFile file) {
        String imageUrl = orderService.uploadProfileImage(orderId, file);
        return ResponseEntity.ok(imageUrl);
    }

    @PostMapping(value = "/{orderId}/arrival-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<String> uploadArrivalPhoto(
            @PathVariable("orderId") Long orderId,
            @RequestParam("image") MultipartFile file,
            @AuthenticationPrincipal Users user
    ) {
        String imageUrl = orderService.uploadArrivalPhoto(orderId, user.getUserId(), file);
        return ResponseEntity.ok(imageUrl);
    }

    @GetMapping("/{orderId}/arrival-photo")
    public ResponseEntity<String> getArrivalPhoto(@PathVariable("orderId") Long orderId) {
        String imageUrl = orderService.getArrivalPhotoUrl(orderId);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 오더 이미지 조회
     * GET /api/v1/orders/{orderId}/image
     */
    @GetMapping("/{orderId}/image")
    public ResponseEntity<String> getOrderImage(@PathVariable("orderId") Long orderId) {
        String imageUrl = orderService.getProfileImageUrl(orderId);
        return ResponseEntity.ok(imageUrl);
    }

    /**
     * 오더 이미지 삭제
     * DELETE /api/v1/orders/{orderId}/image
     */
    @DeleteMapping("/{orderId}/image")
    public ResponseEntity<String> deleteOrderImage(@PathVariable("orderId") Long orderId) {
        orderService.deleteProfileImage(orderId);
        return ResponseEntity.ok("오더 이미지가 삭제되었습니다.");
    }
}
