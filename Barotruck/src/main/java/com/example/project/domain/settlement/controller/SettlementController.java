package com.example.project.domain.settlement.controller;

import com.example.project.domain.settlement.dto.SettlementRegionStatResponse;
import com.example.project.domain.settlement.dto.SettlementResponse;
import com.example.project.domain.settlement.dto.SettlementStatusSummaryResponse;
import com.example.project.domain.settlement.dto.SettlementSummaryResponse;
import com.example.project.domain.settlement.dto.UpdateSettlementStatusRequest;
import com.example.project.domain.settlement.service.SettlementService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SettlementResponse> getOrderSettlement(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(settlementService.getSettlementForOrder(orderId, currentUser));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SettlementResponse>> getMySettlements(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(settlementService.getMySettlements(currentUser, status));
    }

    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SettlementResponse> updateStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody UpdateSettlementStatusRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(settlementService.updateSettlementStatus(orderId, request, currentUser));
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

    @GetMapping("/admin/status-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SettlementStatusSummaryResponse> getStatusSummary() {
        return ApiResponse.ok(settlementService.getSettlementStatusSummary());
    }
}
