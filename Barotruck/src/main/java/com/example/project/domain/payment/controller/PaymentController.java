package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.*;
import com.example.project.domain.payment.dto.paymentResponse.TossPrepareResponse;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransportPaymentService transportPaymentService;
    private final FeeInvoiceService feeInvoiceService;

    @GetMapping(value = "/toss-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> tossTestPage() {
        Resource page = new ClassPathResource("static/toss-test.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @PostMapping("/orders/{orderId}/mark-paid")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<TransportPaymentResponse> markPaid(
            @PathVariable Long orderId,
            @RequestBody MarkPaidRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var p = transportPaymentService.markPaid(
                currentUser,
                orderId,
                request.getMethod(),
                request.getPaymentTiming(),
                request.getProofUrl(),
                request.getPaidAt()
        );
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<TransportPaymentResponse> confirm(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser) {
        var p = transportPaymentService.confirm(currentUser, orderId);
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    @PostMapping("/orders/{orderId}/dispute")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TransportPaymentResponse> dispute(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser) {
        var p = transportPaymentService.dispute(currentUser, orderId);
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }

    @PostMapping("/orders/{orderId}/toss/prepare")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<TossPrepareResponse> prepareTossPayment(
            @PathVariable("orderId") Long orderId,
            @RequestBody TossPrepareRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(transportPaymentService.prepareTossPayment(currentUser, orderId, request));
    }

    @PostMapping("/orders/{orderId}/toss/confirm")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<TransportPaymentResponse> confirmTossPayment(
            @PathVariable("orderId") Long orderId,
            @RequestBody TossConfirmRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var payment = transportPaymentService.confirmTossPayment(currentUser, orderId, request);
        return ApiResponse.ok(TransportPaymentResponse.from(payment));
    }

    @PostMapping("/webhooks/toss")
    public ApiResponse<?> receiveTossWebhook(
            @RequestHeader(value = "Toss-Event-Id", required = false) String eventId,
            @RequestBody String payload
    ) {
        transportPaymentService.handleTossWebhook(eventId, payload);
        return ApiResponse.ok(true);
    }



    @GetMapping("/fee-invoices/me")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<?> myFeeInvoice(
            @RequestParam("period") String period,
            @AuthenticationPrincipal Users currentUser
    ) {
        var invoice = feeInvoiceService.getMyInvoice(currentUser, YearMonth.parse(period));
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/fee-invoices/{invoiceId}/mark-paid")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<?> markFeeInvoicePaid(
            @PathVariable("invoiceId") Long invoiceId,
            @AuthenticationPrincipal Users currentUser
    ) {
        var invoice = feeInvoiceService.markInvoicePaid(currentUser, invoiceId);
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/orders/{orderId}/external-pay")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<TransportPaymentResponse> externalPay(
            @PathVariable("orderId") Long orderId,
            @RequestBody ExternalPayRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var p = transportPaymentService.externalPay(
                currentUser,
                orderId,
                request.getMethod(),
                request.getPaymentTiming()
        );
        return ApiResponse.ok(TransportPaymentResponse.from(p));
    }
}

