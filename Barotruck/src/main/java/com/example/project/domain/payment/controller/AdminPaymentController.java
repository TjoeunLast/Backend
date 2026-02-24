package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.service.core.DriverPayoutService;
import com.example.project.domain.payment.service.core.FeeInvoiceBatchService;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payment")
public class AdminPaymentController {
    private final TransportPaymentService transportPaymentService;
    private final FeeInvoiceBatchService feeInvoiceBatchService;
    private final DriverPayoutService driverPayoutService;
    private final FeeInvoiceService feeInvoiceService;

    // 관리자/배치용 (MVP)
    @PostMapping("/orders/{orderId}/disputes")
    public ApiResponse<PaymentDisputeResponse> createDispute(
            @PathVariable("orderId") Long orderId,
            @RequestBody CreatePaymentDisputeRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {

        var dispute = transportPaymentService.createDispute(currentUser, orderId, request);
        return ApiResponse.ok(PaymentDisputeResponse.from(dispute));
    }

    @PatchMapping("/orders/{orderId}/disputes/{disputeId}/status")
    public ApiResponse<PaymentDisputeResponse> updateDisputeStatus(
            @PathVariable("orderId") Long orderId,
            @PathVariable("disputeId") Long disputeId,
            @RequestBody UpdatePaymentDisputeStatusRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var dispute = transportPaymentService.updateDisputeStatus(currentUser, orderId, disputeId, request);
        return ApiResponse.ok(PaymentDisputeResponse.from(dispute));
    }

    @PostMapping("/fee-invoices/run")
    public ApiResponse<?> runFeeInvoiceBatch(@RequestParam("period") String period) {
        int count = feeInvoiceBatchService.runInvoiceGeneration(YearMonth.parse(period));
        return ApiResponse.ok(count);
    }

    @PostMapping("/fee-invoices/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> generateFeeInvoice(@RequestParam("shipperUserId") Long shipperUserId, @RequestParam String period) {
        var invoice = feeInvoiceService.generateForShipper(shipperUserId, YearMonth.parse(period));
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/payouts/run")
    public ApiResponse<?> runPayoutBatch(@RequestParam("date") String date) {
        return ApiResponse.ok(driverPayoutService.runPayoutForDate(LocalDate.parse(date)));
    }

    @PostMapping("/payout-items/{itemId}/retry")
    public ApiResponse<?> retryPayoutItem(@PathVariable("itemId") Long itemId) {
        return ApiResponse.ok(driverPayoutService.retryItem(itemId));
    }

}
