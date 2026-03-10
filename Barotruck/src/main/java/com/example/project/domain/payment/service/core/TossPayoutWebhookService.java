package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.repository.DriverRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class TossPayoutWebhookService {

    private final DriverPayoutItemRepository payoutItemRepository;
    private final SettlementRepository settlementRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public String handle(String eventType, JsonNode rootNode) {
        JsonNode node = unwrap(rootNode);
        String normalizedEventType = normalize(eventType);

        if (normalizedEventType.contains("PAYOUT")) {
            return handlePayoutChanged(node);
        }
        if (normalizedEventType.contains("SELLER")) {
            return handleSellerChanged(node);
        }
        return "IGNORED";
    }

    private String handlePayoutChanged(JsonNode node) {
        String payoutRef = firstNonBlank(
                readText(node, "payoutId"),
                readText(node, "payoutKey"),
                readText(node, "id"),
                readText(node, "refPayoutId")
        );
        if (isBlank(payoutRef)) {
            return "NO_PAYOUT_REF";
        }

        DriverPayoutItem item = payoutItemRepository.findByPayoutRef(payoutRef).orElse(null);
        if (item == null) {
            return "NO_PAYOUT_ITEM";
        }

        String status = normalize(firstNonBlank(readText(node, "status"), readText(node, "payoutStatus")));
        String reason = firstNonBlank(readText(node, "message"), readText(node, "reason"), readText(node, "code"));

        if (isCompletedStatus(status)) {
            return syncPayoutCompleted(item, payoutRef, "WEBHOOK");
        }
        if (isFailureStatus(status)) {
            return syncPayoutFailed(item, defaultIfBlank(reason, "payout webhook failure"), false, "WEBHOOK");
        }
        if (isRequestedStatus(status)) {
            return syncPayoutAccepted(item, payoutRef, "WEBHOOK");
        }
        return "IGNORED_PAYOUT_STATUS";
    }

    private String handleSellerChanged(JsonNode node) {
        String sellerId = firstNonBlank(readText(node, "sellerId"), readText(node, "id"));
        if (isBlank(sellerId)) {
            return "NO_SELLER_ID";
        }
        String status = normalize(firstNonBlank(readText(node, "status"), readText(node, "sellerStatus")));
        if (isBlank(status)) {
            return "NO_SELLER_STATUS";
        }

        var driverOpt = driverRepository.findByTossPayoutSellerId(sellerId);
        if (driverOpt.isEmpty()) {
            return "NO_DRIVER";
        }

        var driver = driverOpt.get();
        if (sameText(driver.getTossPayoutSellerStatus(), status)) {
            return "NOOP_SELLER_STATUS";
        }
        driver.setTossPayoutSellerStatus(status);
        driverRepository.save(driver);
        return "SYNCED_SELLER";
    }

    public String syncPayoutAccepted(DriverPayoutItem item, String payoutRef, String source) {
        if (item == null) {
            return "NO_PAYOUT_ITEM";
        }
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            return result("NOOP_COMPLETED_TERMINAL", source);
        }

        String resolvedPayoutRef = defaultIfBlank(payoutRef, item.getPayoutRef());
        if (item.getStatus() == PayoutStatus.REQUESTED
                && sameText(item.getPayoutRef(), resolvedPayoutRef)
                && isBlank(item.getFailureReason())) {
            return result("NOOP_PAYOUT_REQUESTED", source);
        }

        item.markAccepted(resolvedPayoutRef);
        payoutItemRepository.save(item);
        refreshBatchStatus(item.getBatch());
        return result("SYNCED_PAYOUT_REQUESTED", source);
    }

    public String syncPayoutCompleted(DriverPayoutItem item, String payoutRef, String source) {
        if (item == null) {
            return "NO_PAYOUT_ITEM";
        }
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            syncSettlementCompletedOnPayout(item.getOrderId());
            return result("NOOP_PAYOUT_COMPLETED", source);
        }

        item.markCompleted(defaultIfBlank(payoutRef, item.getPayoutRef()));
        payoutItemRepository.save(item);
        syncSettlementCompletedOnPayout(item.getOrderId());
        refreshBatchStatus(item.getBatch());
        return result("SYNCED_PAYOUT_COMPLETED", source);
    }

    public String syncPayoutFailed(DriverPayoutItem item, String reason, boolean incrementRetryCount, String source) {
        if (item == null) {
            return "NO_PAYOUT_ITEM";
        }
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            return result("NOOP_COMPLETED_TERMINAL", source);
        }

        String resolvedReason = defaultIfBlank(reason, "unknown payout failure");
        if (item.getStatus() == PayoutStatus.FAILED && sameText(item.getFailureReason(), resolvedReason)) {
            return result("NOOP_PAYOUT_FAILED", source);
        }

        item.markFailed(resolvedReason, incrementRetryCount);
        payoutItemRepository.save(item);
        refreshBatchStatus(item.getBatch());
        return result("SYNCED_PAYOUT_FAILED", source);
    }

    public void refreshBatchStatus(DriverPayoutBatch batch) {
        if (batch == null || batch.getBatchId() == null) {
            return;
        }

        long totalCount = payoutItemRepository.countByBatch_BatchId(batch.getBatchId());
        long pendingCount = payoutItemRepository.countByBatch_BatchIdAndStatusIn(
                batch.getBatchId(),
                List.of(PayoutStatus.READY, PayoutStatus.REQUESTED, PayoutStatus.RETRYING)
        );
        long failedCount = payoutItemRepository.countByBatch_BatchIdAndStatus(batch.getBatchId(), PayoutStatus.FAILED);

        if (totalCount == 0) {
            batch.markCompleted();
            return;
        }
        if (pendingCount > 0) {
            return;
        }
        if (failedCount > 0) {
            batch.markFailed("failed items: " + failedCount);
            return;
        }
        batch.markCompleted();
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

    private boolean isCompletedStatus(String status) {
        return status.contains("COMPLETED") || status.contains("DONE") || status.contains("SUCCESS");
    }

    private boolean isFailureStatus(String status) {
        return status.contains("FAIL") || status.contains("ERROR") || status.contains("REJECT") || status.contains("CANCEL");
    }

    private boolean isRequestedStatus(String status) {
        return status.contains("REQUEST")
                || status.contains("ACCEPT")
                || status.contains("PENDING")
                || status.contains("PROCESS")
                || status.contains("SCHEDULE");
    }

    private void syncSettlementCompletedOnPayout(Long orderId) {
        settlementRepository.findByOrderId(orderId).ifPresent(settlement -> {
            if (settlement.getStatus() != SettlementStatus.WAIT) {
                settlement.setStatus(SettlementStatus.COMPLETED);
            }
            if (settlement.getFeeCompleteDate() == null) {
                settlement.setFeeCompleteDate(LocalDateTime.now());
            }
            settlementRepository.save(settlement);
        });
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean sameText(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
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

    private String result(String action, String source) {
        if (isBlank(source)) {
            return action;
        }
        return action + ":" + normalize(source);
    }
}
