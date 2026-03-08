package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateFeePolicyRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateLevelFeeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.LevelFeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.service.core.DriverPayoutService;
import com.example.project.domain.payment.service.core.FeeInvoiceBatchService;
import com.example.project.domain.payment.service.core.FeePolicyService;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.PaymentReconciliationService;
import com.example.project.domain.payment.service.core.PaymentRetryQueueService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import jakarta.validation.Valid;
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
    private final FeePolicyService feePolicyService;
    private final PaymentReconciliationService paymentReconciliationService;
    private final PaymentRetryQueueService paymentRetryQueueService;

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

    @PostMapping("/toss/expire-prepared/run")
    public ApiResponse<?> runExpirePrepared() {
        return ApiResponse.ok(paymentRetryQueueService.expirePreparedTransactions());
    }

    @PostMapping("/toss/retries/run")
    public ApiResponse<?> runTossRetryQueue() {
        return ApiResponse.ok(paymentRetryQueueService.processFailedRetryQueue());
    }

    @PostMapping("/reconciliation/run")
    public ApiResponse<?> runReconciliation() {
        paymentReconciliationService.runDailyReconciliation();
        return ApiResponse.ok(true);
    }

    @GetMapping("/fee-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeePolicyResponse> getFeePolicy() {
        return ApiResponse.ok(feePolicyService.getCurrentPolicy());
    }

    @GetMapping("/fee-policy/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeePolicyResponse> getCurrentFeePolicy() {
        return ApiResponse.ok(feePolicyService.getCurrentPolicy());
    }

    @GetMapping("/fee-policy/levels/{level}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LevelFeePolicyResponse> getFeePolicyByLevel(
            @PathVariable("level") Long level
    ) {
        return ApiResponse.ok(feePolicyService.getCurrentPolicyByLevel(level));
    }

    @PatchMapping("/fee-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeePolicyResponse> updateFeePolicy(
            @Valid @RequestBody UpdateFeePolicyRequest request
    ) {
        return ApiResponse.ok(feePolicyService.updatePolicy(request));
    }

    @PostMapping("/fee-policy/levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LevelFeePolicyResponse> updateFeePolicyByLevel(
            @Valid @RequestBody UpdateLevelFeeRequest request
    ) {
        return ApiResponse.ok(feePolicyService.updateLevelPolicy(request));
    }

}
