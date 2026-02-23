package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.service.TransportPaymentService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payment")
public class AdminPaymentController {
    private final TransportPaymentService transportPaymentService;
    // 관리자/배치용 (MVP)
    @PostMapping("/orders/{orderId}/disputes")
    public ApiResponse<PaymentDisputeResponse> createDispute(
            @PathVariable Long orderId,
            @RequestBody CreatePaymentDisputeRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {

        var dispute = transportPaymentService.createDispute(currentUser, orderId, request);
        return ApiResponse.ok(PaymentDisputeResponse.from(dispute));
    }

    @PatchMapping("/orders/{orderId}/disputes/{disputeId}/status")
    public ApiResponse<PaymentDisputeResponse> updateDisputeStatus(
            @PathVariable Long orderId,
            @PathVariable Long disputeId,
            @RequestBody UpdatePaymentDisputeStatusRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var dispute = transportPaymentService.updateDisputeStatus(currentUser, orderId, disputeId, request);
        return ApiResponse.ok(PaymentDisputeResponse.from(dispute));
    }
}
