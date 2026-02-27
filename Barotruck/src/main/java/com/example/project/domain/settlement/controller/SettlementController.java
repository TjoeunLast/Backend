package com.example.project.domain.settlement.controller;

import com.example.project.domain.settlement.dto.SettlementRegionStatResponse;
import com.example.project.domain.settlement.dto.SettlementRequest;
import com.example.project.domain.settlement.dto.SettlementResponse;
import com.example.project.domain.settlement.dto.SettlementSummaryResponse;
import com.example.project.domain.settlement.service.SettlementService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // 기존 호환 API
    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public ResponseEntity<String> init(
            @RequestBody SettlementRequest request,
            @AuthenticationPrincipal Users user
    ) {
        settlementService.initiateSettlement(request, user);
        return ResponseEntity.ok("결제 준비가 완료되었습니다.");
    }

    // 기존 호환 API
    @PatchMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public ResponseEntity<String> complete(@PathVariable("orderId") Long orderId) {
        settlementService.completeSettlement(orderId);
        return ResponseEntity.ok("결제가 성공적으로 완료되었습니다.");
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SettlementResponse> getOrderSettlement(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(SettlementResponse.from(settlementService.getSettlementForOrder(orderId, currentUser)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SettlementResponse>> getMySettlements(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(
                settlementService.getMySettlements(currentUser, status).stream()
                        .map(SettlementResponse::from)
                        .toList()
        );
    }

    @PatchMapping("/orders/{orderId}/complete-by-user")
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public ApiResponse<SettlementResponse> completeByUser(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(SettlementResponse.from(settlementService.completeSettlementByUser(orderId, currentUser)));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SettlementSummaryResponse> getSummary(
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ApiResponse.ok(settlementService.getSettlementSummary(start, end));
    }

    @GetMapping("/admin/regions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SettlementRegionStatResponse>> getRegionStats(
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ApiResponse.ok(settlementService.getSettlementRegionStats(start, end));
    }
}
