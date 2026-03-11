package com.example.project.domain.payment.service.query;

import com.example.project.domain.payment.domain.FeeAutoChargeAttempt;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeAutoChargeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentResponse.AdminBillingAgreementStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutBatchStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeAutoChargeAttemptListResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeInvoiceStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.GatewayTransactionStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentReconciliationStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentRetryQueueStatusResponse;
import com.example.project.domain.payment.repository.DriverPayoutBatchRepository;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeAutoChargeAttemptRepository;
import com.example.project.domain.payment.repository.FeeInvoiceItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.PaymentGatewayWebhookEventRepository;
import com.example.project.domain.payment.repository.ShipperBillingAgreementRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.member.domain.Driver;
import com.example.project.member.repository.DriverRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentStatusQueryService {

    private final PaymentDisputeRepository paymentDisputeRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeInvoiceItemRepository feeInvoiceItemRepository;
    private final ShipperBillingAgreementRepository shipperBillingAgreementRepository;
    private final FeeAutoChargeAttemptRepository feeAutoChargeAttemptRepository;
    private final DriverPayoutBatchRepository driverPayoutBatchRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final PaymentGatewayWebhookEventRepository paymentGatewayWebhookEventRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverRepository driverRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.toss.retry.max-attempts:5}")
    private int maxRetryAttempts;

    public PaymentDisputeStatusResponse getDisputeStatus(Long orderId) {
        var dispute = paymentDisputeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("dispute not found. orderId=" + orderId));
        return PaymentDisputeStatusResponse.from(dispute);
    }

    public FeeInvoiceStatusResponse getFeeInvoiceStatus(Long shipperUserId, String period) {
        var invoice = feeInvoiceRepository.findByShipperUserIdAndPeriod(shipperUserId, period)
                .orElseThrow(() -> new IllegalArgumentException(
                        "fee invoice not found. shipperUserId=" + shipperUserId + ", period=" + period
                ));

        var items = feeInvoiceItemRepository.findAllByInvoiceId(invoice.getInvoiceId());
        Map<Long, TransportPayment> paymentsByOrderId = items.isEmpty()
                ? Collections.emptyMap()
                : transportPaymentRepository.findAllByOrderIdIn(
                                items.stream().map(item -> item.getOrderId()).toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(TransportPayment::getOrderId, Function.identity(), (left, right) -> left));
        return FeeInvoiceStatusResponse.from(invoice, items, paymentsByOrderId);
    }

    public AdminBillingAgreementStatusResponse getBillingAgreementStatus(Long shipperUserId) {
        if (shipperUserId == null) {
            throw new IllegalArgumentException("shipperUserId is required");
        }

        ShipperBillingAgreement agreement = shipperBillingAgreementRepository
                .findTopByShipperUserIdOrderByAgreementIdDesc(shipperUserId)
                .orElse(null);
        List<FeeAutoChargeAttempt> recentAttempts = feeAutoChargeAttemptRepository
                .findByShipperUserIdOrderByAttemptIdDesc(shipperUserId, PageRequest.of(0, 10));

        return AdminBillingAgreementStatusResponse.of(
                shipperUserId,
                agreement,
                recentAttempts,
                feeAutoChargeAttemptRepository.countByShipperUserId(shipperUserId),
                feeAutoChargeAttemptRepository.countByShipperUserIdAndStatus(
                        shipperUserId,
                        FeeAutoChargeStatus.SUCCEEDED
                ),
                feeAutoChargeAttemptRepository.countByShipperUserIdAndStatus(
                        shipperUserId,
                        FeeAutoChargeStatus.FAILED
                )
        );
    }

    public FeeAutoChargeAttemptListResponse getFeeAutoChargeAttempts(
            Long shipperUserId,
            Long invoiceId,
            Integer limit
    ) {
        int safeLimit = normalizeLimit(limit);
        if (shipperUserId == null && invoiceId == null) {
            throw new IllegalArgumentException("shipperUserId or invoiceId is required");
        }

        List<FeeAutoChargeAttempt> attempts = invoiceId != null
                ? feeAutoChargeAttemptRepository.findByInvoiceIdOrderByAttemptIdDesc(
                        invoiceId,
                        PageRequest.of(0, safeLimit)
                )
                : feeAutoChargeAttemptRepository.findByShipperUserIdOrderByAttemptIdDesc(
                        shipperUserId,
                        PageRequest.of(0, safeLimit)
                );

        return FeeAutoChargeAttemptListResponse.of(shipperUserId, invoiceId, safeLimit, attempts);
    }

    public DriverPayoutBatchStatusResponse getPayoutBatchStatus(LocalDate date) {
        var batch = driverPayoutBatchRepository.findByBatchDate(date)
                .orElseThrow(() -> new IllegalArgumentException("payout batch not found. date=" + date));
        long totalItems = driverPayoutItemRepository.countByBatch_BatchId(batch.getBatchId());
        long failedItems = driverPayoutItemRepository.countByBatch_BatchIdAndStatus(batch.getBatchId(), PayoutStatus.FAILED);
        return DriverPayoutBatchStatusResponse.of(batch, totalItems, failedItems);
    }

    public DriverPayoutItemStatusResponse getPayoutItemStatusByOrderId(Long orderId) {
        var item = driverPayoutItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("payout item not found. orderId=" + orderId));
        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId).orElse(null);

        Driver driver = driverRepository.findByUser_UserId(item.getDriverUserId()).orElse(null);
        PaymentGatewayWebhookEvent latestPayoutWebhook = findLatestPayoutWebhook(item);
        PaymentGatewayWebhookEvent latestSellerWebhook = findLatestSellerWebhook(driver);
        PaymentGatewayWebhookEvent latestWebhook = pickLatestWebhook(latestPayoutWebhook, latestSellerWebhook);
        String webhookStatus = extractWebhookStatus(latestPayoutWebhook);

        return DriverPayoutItemStatusResponse.from(
                item,
                payment,
                driver,
                latestWebhook,
                webhookStatus,
                matchesPayoutStatus(item.getStatus(), webhookStatus)
        );
    }

    public GatewayTransactionStatusResponse getTossOrderStatus(Long orderId) {
        var tx = paymentGatewayTransactionRepository
                .findTopByOrderIdAndProviderOrderByCreatedAtDesc(orderId, PaymentProvider.TOSS)
                .orElseThrow(() -> new IllegalArgumentException("toss transaction not found. orderId=" + orderId));
        return GatewayTransactionStatusResponse.from(tx);
    }

    public PaymentRetryQueueStatusResponse getExpirePreparedStatus() {
        LocalDateTime now = LocalDateTime.now();
        long candidateCount = paymentGatewayTransactionRepository.countByProviderAndStatusAndExpiresAtBefore(
                PaymentProvider.TOSS,
                GatewayTxStatus.PREPARED,
                now
        );
        LocalDateTime firstTargetAt = paymentGatewayTransactionRepository
                .findTop100ByProviderAndStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentProvider.TOSS,
                        GatewayTxStatus.PREPARED,
                        now
                )
                .stream()
                .findFirst()
                .map(PaymentGatewayTransaction::getExpiresAt)
                .orElse(null);

        return PaymentRetryQueueStatusResponse.builder()
                .status(GatewayTxStatus.PREPARED)
                .candidateCount(candidateCount)
                .firstTargetAt(firstTargetAt)
                .maxRetryAttempts(null)
                .build();
    }

    public PaymentRetryQueueStatusResponse getRetryQueueStatus() {
        LocalDateTime now = LocalDateTime.now();
        long candidateCount = paymentGatewayTransactionRepository.countByProviderAndStatusAndNextRetryAtLessThanEqual(
                PaymentProvider.TOSS,
                GatewayTxStatus.FAILED,
                now
        );
        LocalDateTime firstTargetAt = paymentGatewayTransactionRepository
                .findTop100ByProviderAndStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        PaymentProvider.TOSS,
                        GatewayTxStatus.FAILED,
                        now
                )
                .stream()
                .findFirst()
                .map(PaymentGatewayTransaction::getNextRetryAt)
                .orElse(null);

        return PaymentRetryQueueStatusResponse.builder()
                .status(GatewayTxStatus.FAILED)
                .candidateCount(candidateCount)
                .firstTargetAt(firstTargetAt)
                .maxRetryAttempts(maxRetryAttempts)
                .build();
    }

    public PaymentReconciliationStatusResponse getReconciliationStatus() {
        List<PaymentGatewayTransaction> confirmedGatewayTxs =
                paymentGatewayTransactionRepository.findAllByProviderAndStatus(PaymentProvider.TOSS, GatewayTxStatus.CONFIRMED);

        long unresolvedMismatchCount = 0;
        for (PaymentGatewayTransaction tx : confirmedGatewayTxs) {
            TransportPayment payment = transportPaymentRepository.findByOrderId(tx.getOrderId()).orElse(null);
            if (payment != null && isPaidOrSettledState(payment.getStatus())) {
                continue;
            }
            unresolvedMismatchCount++;
        }

        long confirmedGatewayCount = confirmedGatewayTxs.size();
        long matchedPaymentCount = confirmedGatewayCount - unresolvedMismatchCount;
        return PaymentReconciliationStatusResponse.builder()
                .confirmedGatewayCount(confirmedGatewayCount)
                .matchedPaymentCount(matchedPaymentCount)
                .unresolvedMismatchCount(unresolvedMismatchCount)
                .build();
    }

    private boolean isPaidOrSettledState(TransportPaymentStatus status) {
        if (status == null) {
            return false;
        }
        return status == TransportPaymentStatus.PAID
                || status == TransportPaymentStatus.CONFIRMED
                || status == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
                || status == TransportPaymentStatus.DISPUTED
                || status == TransportPaymentStatus.ADMIN_HOLD
                || status == TransportPaymentStatus.ADMIN_REJECTED;
    }

    private PaymentGatewayWebhookEvent findLatestPayoutWebhook(com.example.project.domain.payment.domain.DriverPayoutItem item) {
        if (item == null || isBlank(item.getPayoutRef())) {
            return null;
        }
        return findLatestWebhookByKeyword("payout", item.getPayoutRef());
    }

    private PaymentGatewayWebhookEvent findLatestSellerWebhook(Driver driver) {
        if (driver == null) {
            return null;
        }
        PaymentGatewayWebhookEvent bySellerId = findLatestWebhookByKeyword("seller", driver.getTossPayoutSellerId());
        PaymentGatewayWebhookEvent bySellerRef = findLatestWebhookByKeyword("seller", driver.getTossPayoutSellerRef());
        return pickLatestWebhook(bySellerId, bySellerRef);
    }

    private PaymentGatewayWebhookEvent findLatestWebhookByKeyword(String eventKeyword, String payloadKeyword) {
        if (isBlank(eventKeyword) || isBlank(payloadKeyword)) {
            return null;
        }

        List<PaymentGatewayWebhookEvent> events =
                paymentGatewayWebhookEventRepository
                        .findByProviderAndEventTypeContainingIgnoreCaseAndPayloadContainingIgnoreCaseOrderByReceivedAtDesc(
                                PaymentProvider.TOSS,
                                eventKeyword,
                                payloadKeyword,
                                PageRequest.of(0, 1)
                        );
        return events.isEmpty() ? null : events.get(0);
    }

    private PaymentGatewayWebhookEvent pickLatestWebhook(
            PaymentGatewayWebhookEvent left,
            PaymentGatewayWebhookEvent right
    ) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }

        LocalDateTime leftTime = preferredWebhookTime(left);
        LocalDateTime rightTime = preferredWebhookTime(right);

        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private LocalDateTime preferredWebhookTime(PaymentGatewayWebhookEvent event) {
        if (event == null) {
            return null;
        }
        return event.getProcessedAt() == null ? event.getReceivedAt() : event.getProcessedAt();
    }

    private String extractWebhookStatus(PaymentGatewayWebhookEvent webhookEvent) {
        if (webhookEvent == null || isBlank(webhookEvent.getPayload())) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(webhookEvent.getPayload());
            JsonNode node = unwrap(root);
            return firstNonBlank(
                    readText(node, "status"),
                    readText(node, "payoutStatus"),
                    readText(node, "sellerStatus")
            );
        } catch (Exception e) {
            log.debug("failed to parse webhook payload. webhookId={}", webhookEvent.getWebhookId(), e);
            return null;
        }
    }

    private Boolean matchesPayoutStatus(PayoutStatus payoutStatus, String webhookStatus) {
        if (payoutStatus == null || isBlank(webhookStatus)) {
            return null;
        }

        String normalizedWebhookStatus = normalize(webhookStatus);
        return switch (payoutStatus) {
            case READY -> normalizedWebhookStatus.contains("READY");
            case REQUESTED ->
                    normalizedWebhookStatus.contains("REQUEST")
                            || normalizedWebhookStatus.contains("ACCEPT");
            case COMPLETED ->
                    normalizedWebhookStatus.contains("COMPLETE")
                            || normalizedWebhookStatus.contains("DONE")
                            || normalizedWebhookStatus.contains("SUCCESS");
            case FAILED ->
                    normalizedWebhookStatus.contains("FAIL")
                            || normalizedWebhookStatus.contains("ERROR")
                            || normalizedWebhookStatus.contains("REJECT")
                            || normalizedWebhookStatus.contains("CANCEL");
            case RETRYING ->
                    normalizedWebhookStatus.contains("RETRY")
                            || normalizedWebhookStatus.contains("REQUEST")
                            || normalizedWebhookStatus.contains("ACCEPT");
        };
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 100);
    }

    private JsonNode unwrap(JsonNode rootNode) {
        if (rootNode == null || rootNode.isNull()) {
            return null;
        }
        JsonNode dataNode = rootNode.get("data");
        if (dataNode != null && !dataNode.isNull() && dataNode.isObject()) {
            return dataNode;
        }
        return rootNode;
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null ? null : text.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
