// src/main/java/com/example/project/domain/payment/controller/PaymentController.java
package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.MarkPaidRequest;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.service.FeeInvoiceService;
import com.example.project.domain.payment.service.TransportPaymentService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransportPaymentService transportPaymentService;
    private final FeeInvoiceService feeInvoiceService;

    @PostMapping("/orders/{orderId}/mark-paid")
    public ApiResponse<TransportPaymentResponse> markPaid(
            @PathVariable Long orderId,
            @RequestBody MarkPaidRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var p = transportPaymentService.markPaid(currentUser, orderId, request.getMethod(), request.getProofUrl(), request.getPaidAt());
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ApiResponse<TransportPaymentResponse> confirm(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Users currentUser) {
        var p = transportPaymentService.confirm(currentUser, orderId);
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    @PostMapping("/orders/{orderId}/dispute")
    public ApiResponse<TransportPaymentResponse> dispute(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Users currentUser) {
        var p = transportPaymentService.dispute(currentUser, orderId);
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    // 관리자/배치용 (MVP)
    @PostMapping("/fee-invoices/generate")
    public ApiResponse<?> generateFeeInvoice(@RequestParam Long shipperUserId, @RequestParam String period) {
        var invoice = feeInvoiceService.generateForShipper(shipperUserId, YearMonth.parse(period));
        return ApiResponse.ok(invoice);
    }

    @GetMapping("/fee-invoices/me")
    public ApiResponse<?> myFeeInvoice(
            @RequestParam String period,
            @AuthenticationPrincipal Users currentUser) {
        var invoice = feeInvoiceService.getMyInvoice(currentUser, YearMonth.parse(period));
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/fee-invoices/{invoiceId}/mark-paid")
    public ApiResponse<?> markFeeInvoicePaid(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal Users currentUser) {
        var invoice = feeInvoiceService.markInvoicePaid(currentUser, invoiceId);
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/orders/{orderId}/external-pay")
    public ApiResponse<TransportPaymentResponse> externalPay(
            @PathVariable Long orderId,
            @RequestBody com.example.project.domain.payment.dto.paymentRequest.ExternalPayRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var p = transportPaymentService.externalPay(currentUser, orderId, request.getMethod());
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

}
