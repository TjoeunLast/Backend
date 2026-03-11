package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.*;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeInvoiceStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.ShipperBillingAgreementResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossBillingContextResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPrepareResponse;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.ShipperBillingAgreementService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.domain.payment.service.query.AdminPaymentStatusQueryService;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransportPaymentService transportPaymentService;
    private final ShipperBillingAgreementService shipperBillingAgreementService;
    private final FeeInvoiceService feeInvoiceService;
    private final AdminPaymentStatusQueryService adminPaymentStatusQueryService;
    private final OrderRepository orderRepository;
    private final PaymentDisputeRepository paymentDisputeRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final UsersRepository usersRepository;

    @GetMapping(value = "/toss-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> tossTestPage() {
        Resource page = new ClassPathResource("static/toss-test.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(value = "/toss-live-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> tossLiveTestPage() {
        Resource page = new ClassPathResource("static/toss-live-test.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(value = "/admin-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> adminPaymentTestPage() {
        Resource page = new ClassPathResource("static/admin-payment-test.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(value = "/admin-cycles", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> adminPaymentCycleIndexPage() {
        Resource page = new ClassPathResource("static/admin-payment-cycles.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(
            value = {
                    "/admin-cycle-a",
                    "/admin-cycle-b",
                    "/admin-cycle-c",
                    "/admin-cycle-d",
                    "/admin-cycle-e",
                    "/admin-cycle-f"
            },
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Resource> adminPaymentCyclePage() {
        Resource page = new ClassPathResource("static/admin-payment-cycle.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(value = "/admin-cycle.js", produces = "application/javascript")
    public ResponseEntity<Resource> adminPaymentCycleScript() {
        Resource script = new ClassPathResource("static/admin-payment-cycle.js");
        return ResponseEntity.ok().contentType(MediaType.valueOf("application/javascript")).body(script);
    }

    @GetMapping(value = "/api-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> paymentApiTestPage() {
        Resource page = new ClassPathResource("static/payment-api-test.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    @GetMapping(value = "/api-test.js", produces = "application/javascript")
    public ResponseEntity<Resource> paymentApiTestScript() {
        Resource script = new ClassPathResource("static/payment-api-test.js");
        return ResponseEntity.ok().contentType(MediaType.valueOf("application/javascript")).body(script);
    }

    @GetMapping(value = "/api-test-config.js", produces = "application/javascript")
    public ResponseEntity<Resource> paymentApiTestConfigScript() {
        Resource script = new ClassPathResource("static/payment-api-test-config.js");
        return ResponseEntity.ok().contentType(MediaType.valueOf("application/javascript")).body(script);
    }

    @GetMapping("/api-test/context")
    public ApiResponse<Map<String, Object>> getPaymentApiTestContext(
            @RequestParam(value = "orderId", required = false) Long orderId
    ) {
        Long resolvedOrderId = orderId;
        if (resolvedOrderId == null) {
            resolvedOrderId = orderRepository.findTopByOrderByOrderIdDesc().map(o -> o.getOrderId()).orElse(null);
        }

        Long disputeId = null;
        Long invoiceId = null;
        Long itemId = null;
        Long shipperId = null;
        Long driverUserId = null;
        String pgOrderId = null;
        String paymentKey = null;
        String amount = null;

        if (resolvedOrderId != null) {
            var orderOpt = orderRepository.findById(resolvedOrderId);
            if (orderOpt.isPresent()) {
                var order = orderOpt.get();
                shipperId = order.getUser() == null ? null : order.getUser().getUserId();
                driverUserId = order.getDriverNo();
            }

            disputeId = paymentDisputeRepository.findByOrderId(resolvedOrderId)
                    .map(d -> d.getDisputeId())
                    .orElse(null);

            itemId = driverPayoutItemRepository.findByOrderId(resolvedOrderId)
                    .map(i -> i.getItemId())
                    .orElse(null);

            if (shipperId != null) {
                invoiceId = feeInvoiceRepository.findAllByShipperUserIdOrderByPeriodDesc(shipperId).stream()
                        .findFirst()
                        .map(i -> i.getInvoiceId())
                        .orElse(null);
            }

            var txOpt = paymentGatewayTransactionRepository
                    .findTopByOrderIdAndProviderOrderByCreatedAtDesc(resolvedOrderId, PaymentProvider.TOSS);
            if (txOpt.isPresent()) {
                var tx = txOpt.get();
                pgOrderId = tx.getPgOrderId();
                paymentKey = tx.getPaymentKey();
                amount = tx.getAmount() == null ? null : tx.getAmount().toPlainString();
            }
        } else {
            disputeId = paymentDisputeRepository.findTopByOrderByDisputeIdDesc().map(d -> d.getDisputeId()).orElse(null);
            invoiceId = feeInvoiceRepository.findTopByOrderByInvoiceIdDesc().map(i -> i.getInvoiceId()).orElse(null);
            itemId = driverPayoutItemRepository.findTopByOrderByItemIdDesc().map(i -> i.getItemId()).orElse(null);
            shipperId = usersRepository.findTopByRoleOrderByUserIdDesc(Role.SHIPPER).map(u -> u.getUserId()).orElse(null);
            driverUserId = null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", resolvedOrderId);
        data.put("disputeId", disputeId);
        data.put("invoiceId", invoiceId);
        data.put("shipperId", shipperId);
        data.put("itemId", itemId);
        data.put("driverUserId", driverUserId);
        data.put("pgOrderId", pgOrderId);
        data.put("paymentKey", paymentKey);
        data.put("amount", amount);

        return ApiResponse.ok(data);
    }

    @PostMapping("/fee-preview")
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public ApiResponse<FeeBreakdownPreviewResponse> previewFee(
            @RequestBody FeePreviewRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(transportPaymentService.previewFee(currentUser, request));
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

    @PostMapping("/orders/{orderId}/disputes")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ApiResponse<PaymentDisputeResponse> createDispute(
            @PathVariable("orderId") Long orderId,
            @RequestBody CreatePaymentDisputeRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        var dispute = transportPaymentService.createDispute(currentUser, orderId, request);
        return ApiResponse.ok(PaymentDisputeResponse.from(dispute));
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
            @RequestHeader(value = "Toss-Webhook-Secret", required = false) String webhookSecret,
            @RequestBody String payload
    ) {
        transportPaymentService.handleTossWebhook(eventId, payload, webhookSecret);
        return ApiResponse.ok(true);
    }

    @GetMapping("/billing/context")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<TossBillingContextResponse> getBillingContext(
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(shipperBillingAgreementService.getBillingContext(currentUser));
    }

    @PostMapping("/billing/agreements")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperBillingAgreementResponse> issueBillingAgreement(
            @RequestBody TossBillingIssueRequest request,
            @AuthenticationPrincipal Users currentUser
    ) {
        validateBillingIssueRequest(request);
        return ApiResponse.ok(shipperBillingAgreementService.issueBillingAgreement(currentUser, request));
    }

    @GetMapping("/billing/agreements/me")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperBillingAgreementResponse> getMyBillingAgreement(
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(shipperBillingAgreementService.getMyAgreement(currentUser));
    }

    @DeleteMapping("/billing/agreements/me")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperBillingAgreementResponse> deactivateMyBillingAgreement(
            @AuthenticationPrincipal Users currentUser
    ) {
        return ApiResponse.ok(shipperBillingAgreementService.deactivateMyAgreement(currentUser));
    }

    @GetMapping("/payouts/orders/{orderId}/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DriverPayoutItemStatusResponse> getMyPayoutStatus(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Users currentUser
    ) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }

        var payoutItem = driverPayoutItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("payout item not found. orderId=" + orderId));

        if (!Objects.equals(payoutItem.getDriverUserId(), currentUser.getUserId())) {
            throw new IllegalStateException("not allowed to access payout status for this order");
        }

        return ApiResponse.ok(adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(orderId));
    }

    @GetMapping("/fee-invoices/me")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<FeeInvoiceStatusResponse> myFeeInvoice(
            @RequestParam("period") String period,
            @AuthenticationPrincipal Users currentUser
    ) {
        YearMonth parsedPeriod = YearMonth.parse(period);
        feeInvoiceService.getMyInvoice(currentUser, parsedPeriod);
        return ApiResponse.ok(
                adminPaymentStatusQueryService.getFeeInvoiceStatus(currentUser.getUserId(), parsedPeriod.toString())
        );
    }

    @PostMapping("/fee-invoices/{invoiceId}/mark-paid")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<FeeInvoiceStatusResponse> markFeeInvoicePaid(
            @PathVariable("invoiceId") Long invoiceId,
            @AuthenticationPrincipal Users currentUser
    ) {
        var invoice = feeInvoiceService.markInvoicePaid(currentUser, invoiceId);
        return ApiResponse.ok(
                adminPaymentStatusQueryService.getFeeInvoiceStatus(invoice.getShipperUserId(), invoice.getPeriod())
        );
    }

    private void validateBillingIssueRequest(TossBillingIssueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }

        request.setAuthKey(normalize(request.getAuthKey()));
        request.setCustomerKey(normalize(request.getCustomerKey()));

        if (isBlank(request.getAuthKey())) {
            throw new IllegalArgumentException("authKey is required");
        }
        if (request.getAuthKey().length() > 200) {
            throw new IllegalArgumentException("authKey must be 200 characters or fewer");
        }
        if (request.getCustomerKey() != null && request.getCustomerKey().length() > 120) {
            throw new IllegalArgumentException("customerKey must be 120 characters or fewer");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

