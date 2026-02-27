package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.*;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPrepareResponse;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransportPaymentService transportPaymentService;
    private final FeeInvoiceService feeInvoiceService;
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
}

