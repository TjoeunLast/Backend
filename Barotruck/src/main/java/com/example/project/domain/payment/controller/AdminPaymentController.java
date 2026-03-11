package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.AdminFeePreviewRequest;
import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.CancelTossPaymentRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateFeePolicyRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateLevelFeeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateTransportPaymentStatusRequest;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutBatchStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.AdminBillingAgreementStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeAutoChargeAttemptListResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeInvoiceStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.GatewayTransactionStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.LevelFeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentReconciliationStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentRetryQueueStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPaymentComparisonResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPaymentLookupResponse;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.service.core.DriverPayoutService;
import com.example.project.domain.payment.service.core.FeeInvoiceBatchService;
import com.example.project.domain.payment.service.core.FeePolicyService;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.MarketplaceFeeCalculationService;
import com.example.project.domain.payment.service.core.PaymentReconciliationService;
import com.example.project.domain.payment.service.core.PaymentRetryQueueService;
import com.example.project.domain.payment.service.core.TossPaymentOpsService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.domain.payment.service.query.AdminPaymentStatusQueryService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payment")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {
    private final TransportPaymentService transportPaymentService;
    private final FeeInvoiceBatchService feeInvoiceBatchService;
    private final DriverPayoutService driverPayoutService;
    private final FeeInvoiceService feeInvoiceService;
    private final FeePolicyService feePolicyService;
    private final MarketplaceFeeCalculationService marketplaceFeeCalculationService;
    private final PaymentReconciliationService paymentReconciliationService;
    private final PaymentRetryQueueService paymentRetryQueueService;
    private final AdminPaymentStatusQueryService adminPaymentStatusQueryService;
    private final TossPaymentOpsService tossPaymentOpsService;

    // 관리자/배치용 (MVP)
    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TransportPaymentResponse> updateTransportPaymentStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody UpdateTransportPaymentStatusRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var payment = transportPaymentService.updateAdminStatus(currentUser, orderId, request);
        return ApiResponse.ok(TransportPaymentResponse.from(payment));
    }

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

    @GetMapping("/orders/{orderId}/disputes/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentDisputeStatusResponse> getDisputeStatus(
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getDisputeStatus(orderId));
    }

    @PostMapping("/fee-invoices/run")
    public ApiResponse<?> runFeeInvoiceBatch(@RequestParam("period") String period) {
        int count = feeInvoiceBatchService.runInvoiceGeneration(YearMonth.parse(period));
        return ApiResponse.ok(count);
    }

    @PostMapping("/fee-invoices/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeInvoiceStatusResponse> generateFeeInvoice(
            @RequestParam("shipperUserId") Long shipperUserId,
            @RequestParam String period
    ) {
        var invoice = feeInvoiceService.generateForShipper(shipperUserId, YearMonth.parse(period));
        return ApiResponse.ok(
                adminPaymentStatusQueryService.getFeeInvoiceStatus(invoice.getShipperUserId(), invoice.getPeriod())
        );
    }

    @GetMapping("/fee-invoices/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeInvoiceStatusResponse> getFeeInvoiceStatus(
            @RequestParam("shipperUserId") Long shipperUserId,
            @RequestParam("period") String period
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getFeeInvoiceStatus(shipperUserId, period));
    }

    @GetMapping("/billing-agreements/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminBillingAgreementStatusResponse> getBillingAgreementStatus(
            @RequestParam("shipperUserId") Long shipperUserId
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getBillingAgreementStatus(shipperUserId));
    }

    @GetMapping("/fee-auto-charge-attempts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeAutoChargeAttemptListResponse> getFeeAutoChargeAttempts(
            @RequestParam(value = "shipperUserId", required = false) Long shipperUserId,
            @RequestParam(value = "invoiceId", required = false) Long invoiceId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.ok(
                adminPaymentStatusQueryService.getFeeAutoChargeAttempts(shipperUserId, invoiceId, limit)
        );
    }

    @PostMapping("/payouts/run")
    public ApiResponse<?> runPayoutBatch(@RequestParam("date") String date) {
        return ApiResponse.ok(driverPayoutService.runPayoutForDate(LocalDate.parse(date)));
    }

    @GetMapping("/payouts/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverPayoutBatchStatusResponse> getPayoutBatchStatus(
            @RequestParam("date") String date
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getPayoutBatchStatus(LocalDate.parse(date)));
    }

    @PostMapping("/orders/{orderId}/payouts/request")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverPayoutItemStatusResponse> requestPayoutForOrder(
            @PathVariable("orderId") Long orderId
    ) {
        validatePositiveId(orderId, "orderId");
        driverPayoutService.requestPayoutForOrder(orderId);
        return ApiResponse.ok(adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(orderId));
    }

    @PostMapping("/payout-items/{itemId}/retry")
    public ApiResponse<?> retryPayoutItem(@PathVariable("itemId") Long itemId) {
        return ApiResponse.ok(driverPayoutService.retryItem(itemId));
    }

    @PostMapping("/payout-items/orders/{orderId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverPayoutItemStatusResponse> syncPayoutItemStatus(
            @PathVariable("orderId") Long orderId
    ) {
        validatePositiveId(orderId, "orderId");
        driverPayoutService.syncPayoutStatusByOrderId(orderId);
        return ApiResponse.ok(adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(orderId));
    }

    @GetMapping("/payout-items/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverPayoutItemStatusResponse> getPayoutItemStatus(
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(orderId));
    }

    @PostMapping("/toss/expire-prepared/run")
    public ApiResponse<?> runExpirePrepared() {
        return ApiResponse.ok(paymentRetryQueueService.expirePreparedTransactions());
    }

    @GetMapping("/toss/expire-prepared/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentRetryQueueStatusResponse> getExpirePreparedStatus() {
        return ApiResponse.ok(adminPaymentStatusQueryService.getExpirePreparedStatus());
    }

    @PostMapping("/toss/retries/run")
    public ApiResponse<?> runTossRetryQueue() {
        return ApiResponse.ok(paymentRetryQueueService.processFailedRetryQueue());
    }

    @GetMapping("/toss/retries/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentRetryQueueStatusResponse> getTossRetryQueueStatus() {
        return ApiResponse.ok(adminPaymentStatusQueryService.getRetryQueueStatus());
    }

    @GetMapping("/toss/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GatewayTransactionStatusResponse> getTossOrderStatus(
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.ok(adminPaymentStatusQueryService.getTossOrderStatus(orderId));
    }

    @GetMapping("/toss/payments/{paymentKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TossPaymentLookupResponse> lookupTossPaymentByPaymentKey(
            @PathVariable("paymentKey") String paymentKey
    ) {
        String normalizedPaymentKey = normalize(paymentKey);
        if (normalizedPaymentKey == null) {
            throw new IllegalArgumentException("paymentKey is required");
        }
        if (normalizedPaymentKey.length() > 200) {
            throw new IllegalArgumentException("paymentKey must be 200 characters or fewer");
        }
        return ApiResponse.ok(tossPaymentOpsService.lookupByPaymentKey(normalizedPaymentKey));
    }

    @GetMapping("/toss/orders/{orderId}/lookup")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TossPaymentComparisonResponse> lookupTossPaymentByOrderId(
            @PathVariable("orderId") Long orderId
    ) {
        validatePositiveId(orderId, "orderId");
        return ApiResponse.ok(tossPaymentOpsService.lookupByOrderId(orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TransportPaymentResponse> cancelTossOrderPayment(
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) CancelTossPaymentRequest request
    ) {
        validatePositiveId(orderId, "orderId");
        validateCancelRequest(request);
        var payment = tossPaymentOpsService.cancelOrderPayment(orderId, request);
        return ApiResponse.ok(TransportPaymentResponse.from(payment));
    }

    @PostMapping("/reconciliation/run")
    public ApiResponse<?> runReconciliation() {
        paymentReconciliationService.runDailyReconciliation();
        return ApiResponse.ok(true);
    }

    @GetMapping("/reconciliation/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentReconciliationStatusResponse> getReconciliationStatus() {
        return ApiResponse.ok(adminPaymentStatusQueryService.getReconciliationStatus());
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

    @PostMapping("/fee-policy/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeBreakdownPreviewResponse> previewFeePolicy(
            @Valid @RequestBody AdminFeePreviewRequest request
    ) {
        validatePreviewLevel(request.getShipperLevel(), "shipperLevel");
        validatePreviewLevel(request.getDriverLevel(), "driverLevel");

        return ApiResponse.ok(
                marketplaceFeeCalculationService.calculate(
                        MarketplaceFeeCalculationService.CalculationCommand.builder()
                                .baseAmount(request.getBaseAmount())
                                .shipperUserLevel(defaultLevel(request.getShipperLevel()))
                                .driverUserLevel(defaultLevel(request.getDriverLevel()))
                                .shipperPromoEligible(Boolean.TRUE.equals(request.getShipperPromoApplied()))
                                .driverPromoEligible(Boolean.TRUE.equals(request.getDriverPromoApplied()))
                                .includeTossFee(request.getIncludeTossFee() == null || request.getIncludeTossFee())
                                .build()
                )
        );
    }

    @PostMapping("/fee-policy/levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LevelFeePolicyResponse> updateFeePolicyByLevel(
            @Valid @RequestBody UpdateLevelFeeRequest request
    ) {
        return ApiResponse.ok(feePolicyService.updateLevelPolicy(request));
    }

    private void validatePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number");
        }
    }

    private void validatePreviewLevel(Long value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be 0 or greater");
        }
    }

    private Long defaultLevel(Long value) {
        return value == null ? 0L : value;
    }

    private void validateCancelRequest(CancelTossPaymentRequest request) {
        if (request == null) {
            return;
        }

        request.setCancelReason(normalize(request.getCancelReason()));

        if (request.getCancelReason() != null && request.getCancelReason().length() > 1000) {
            throw new IllegalArgumentException("cancelReason must be 1000 characters or fewer");
        }

        BigDecimal cancelAmount = request.getCancelAmount();
        if (cancelAmount != null && cancelAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("cancelAmount must be greater than 0");
        }
        if (cancelAmount != null && cancelAmount.scale() > 2) {
            throw new IllegalArgumentException("cancelAmount must have at most 2 decimal places");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
