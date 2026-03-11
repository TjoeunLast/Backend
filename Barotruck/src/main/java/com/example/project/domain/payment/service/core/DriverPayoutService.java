package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.repository.DriverPayoutBatchRepository;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverPayoutService {

    private final DriverPayoutBatchRepository batchRepository;
    private final DriverPayoutItemRepository itemRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverPayoutGatewayClient payoutGatewayClient;
    private final EntityManager entityManager;
    private final TossPayoutWebhookService tossPayoutWebhookService;
    private final PromotionEligibilityService promotionEligibilityService;

    @Scheduled(cron = "${payment.payout.cron:0 0 4 * * *}")
    @Transactional
    public void scheduleDailyPayout() {
        syncRequestedPayouts();
        runPayoutForDate(LocalDate.now());
    }

    @Scheduled(cron = "${payment.payout.sync-cron:0 */15 * * * *}")
    @Transactional
    public void syncRequestedPayouts() {
        List<DriverPayoutItem> requestedItems = itemRepository.findAllByStatusIn(List.of(PayoutStatus.REQUESTED));
        for (DriverPayoutItem item : requestedItems) {
            if (item.getStatus() != PayoutStatus.REQUESTED || item.getPayoutRef() == null || item.getPayoutRef().isBlank()) {
                continue;
            }

            var result = payoutGatewayClient.getPayoutStatus(item.getPayoutRef());
            if (!result.success()) {
                log.debug("skip payout status sync. itemId={}, reason={}", item.getItemId(), result.failReason());
                continue;
            }

            if (result.completed()) {
                tossPayoutWebhookService.syncPayoutCompleted(item, item.getPayoutRef(), "POLLING");
            } else if (result.failed()) {
                tossPayoutWebhookService.syncPayoutFailed(
                        item,
                        defaultFailureReason(result.failReason(), result.gatewayStatus()),
                        false,
                        "POLLING"
                );
            } else {
                continue;
            }
        }
    }

    @Transactional
    public DriverPayoutBatch runPayoutForDate(LocalDate payoutDate) {
        DriverPayoutBatch batch = findOrCreateBatch(payoutDate);

        List<TransportPayment> payments = transportPaymentRepository.findAllByStatusIn(List.of(
                TransportPaymentStatus.CONFIRMED,
                TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
        ));

        for (TransportPayment payment : payments) {
            if (payment.getConfirmedAt() == null || payment.getConfirmedAt().toLocalDate().isAfter(payoutDate)) {
                continue;
            }
            if (!isPayoutEligible(payment)) {
                continue;
            }
            if (payment.getDriverUserId() == null || itemRepository.existsByOrderId(payment.getOrderId())) {
                continue;
            }

            DriverPayoutItem item = itemRepository.save(createReadyItem(batch, payment));
            processItem(item);
        }
        tossPayoutWebhookService.refreshBatchStatus(batch);
        return batch;
    }

    @Transactional
    public DriverPayoutItem requestPayoutForOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be a positive number");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));
        validateManualPayoutEligibility(payment);

        DriverPayoutItem existingItem = itemRepository.findByOrderId(orderId).orElse(null);
        if (existingItem != null) {
            return reprocessExistingItem(existingItem);
        }

        DriverPayoutBatch batch = findOrCreateBatch(LocalDate.now());
        DriverPayoutItem item = itemRepository.save(createReadyItem(batch, payment));
        return processItem(item);
    }

    @Transactional
    public void tryAutoRequestPayoutForOrder(Long orderId, String triggerSource) {
        String resolvedTriggerSource = (triggerSource == null || triggerSource.isBlank())
                ? "AUTO"
                : triggerSource.trim();

        if (orderId == null || orderId <= 0) {
            log.warn("skip auto payout trigger. invalid orderId={}, trigger={}", orderId, resolvedTriggerSource);
            return;
        }

        try {
            DriverPayoutItem item = requestPayoutForOrder(orderId);
            log.info(
                    "auto payout trigger handled. orderId={}, trigger={}, itemId={}, status={}",
                    orderId,
                    resolvedTriggerSource,
                    item != null ? item.getItemId() : null,
                    item != null ? item.getStatus() : null
            );
        } catch (Exception e) {
            log.warn(
                    "auto payout trigger failed. orderId={}, trigger={}, reason={}",
                    orderId,
                    resolvedTriggerSource,
                    e.getMessage()
            );
        }
    }

    @Transactional
    public DriverPayoutItem syncPayoutStatusByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be a positive number");
        }

        DriverPayoutItem item = itemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("payout item not found. orderId=" + orderId));
        return syncExistingItem(item, "MANUAL_SYNC");
    }

    @Transactional
    public DriverPayoutItem retryItem(Long itemId) {
        DriverPayoutItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("payout item not found. itemId=" + itemId));
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            return item;
        }
        item.markRetrying();
        itemRepository.save(item);
        return processItem(item);
    }

    private DriverPayoutItem processItem(DriverPayoutItem item) {
        item.markRequested();
        itemRepository.save(item);

        var result = payoutGatewayClient.payout(
                item.getOrderId(),
                item.getDriverUserId(),
                item.getNetAmount(),
                item.getBatch().getBatchId(),
                item.getItemId()
        );
        if (result.success()) {
            if (result.completed()) {
                tossPayoutWebhookService.syncPayoutCompleted(item, result.payoutRef(), "REQUEST");
                log.info("driver payout completed. orderId={}, itemId={}", item.getOrderId(), item.getItemId());
            } else {
                tossPayoutWebhookService.syncPayoutAccepted(item, result.payoutRef(), "REQUEST");
                log.info(
                        "driver payout requested. orderId={}, itemId={}, payoutRef={}, status={}",
                        item.getOrderId(),
                        item.getItemId(),
                        result.payoutRef(),
                        result.gatewayStatus()
                );
            }
        } else {
            tossPayoutWebhookService.syncPayoutFailed(item, result.failReason(), true, "REQUEST");
            log.warn("driver payout failed. orderId={}, itemId={}, reason={}",
                    item.getOrderId(), item.getItemId(), result.failReason());
        }
        return item;
    }

    private DriverPayoutItem reprocessExistingItem(DriverPayoutItem item) {
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            return item;
        }
        if (item.getStatus() == PayoutStatus.REQUESTED && item.getPayoutRef() != null && !item.getPayoutRef().isBlank()) {
            return syncExistingItem(item, "RECHECK");
        }
        if (item.getStatus() == PayoutStatus.READY || item.getStatus() == PayoutStatus.FAILED) {
            if (item.getStatus() == PayoutStatus.FAILED) {
                item.markRetrying();
                itemRepository.save(item);
            }
            return processItem(item);
        }
        if (item.getStatus() == PayoutStatus.RETRYING) {
            return processItem(item);
        }
        return item;
    }

    private DriverPayoutItem syncExistingItem(DriverPayoutItem item, String source) {
        if (item.getStatus() == PayoutStatus.COMPLETED) {
            return item;
        }
        if (item.getPayoutRef() == null || item.getPayoutRef().isBlank()) {
            return item;
        }

        var result = payoutGatewayClient.getPayoutStatus(item.getPayoutRef());
        if (!result.success()) {
            throw new IllegalStateException(defaultFailureReason(result.failReason(), result.gatewayStatus()));
        }

        if (result.completed()) {
            tossPayoutWebhookService.syncPayoutCompleted(item, item.getPayoutRef(), source);
        } else if (result.failed()) {
            tossPayoutWebhookService.syncPayoutFailed(
                    item,
                    defaultFailureReason(result.failReason(), result.gatewayStatus()),
                    false,
                    source
            );
        } else {
            tossPayoutWebhookService.syncPayoutAccepted(item, item.getPayoutRef(), source);
        }

        entityManager.flush();
        return itemRepository.findById(item.getItemId()).orElse(item);
    }

    private DriverPayoutBatch findOrCreateBatch(LocalDate payoutDate) {
        return batchRepository.findByBatchDate(payoutDate)
                .orElseGet(() -> batchRepository.save(DriverPayoutBatch.start(payoutDate)));
    }

    private DriverPayoutItem createReadyItem(DriverPayoutBatch batch, TransportPayment payment) {
        DriverPayoutItem item = DriverPayoutItem.ready(batch, payment);
        promotionEligibilityService.applyDriverFirstTransportPromotion(item);
        return item;
    }

    private void validateManualPayoutEligibility(TransportPayment payment) {
        if (payment.getDriverUserId() == null) {
            throw new IllegalStateException("driver user not found for payout request");
        }
        if (payment.getConfirmedAt() == null) {
            throw new IllegalStateException("transport payment is not confirmed yet");
        }
        if (payment.getStatus() != TransportPaymentStatus.CONFIRMED
                && payment.getStatus() != TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            throw new IllegalStateException("only confirmed transport payment can request payout");
        }
        if (!isPayoutEligible(payment)) {
            throw new IllegalStateException("payment method is not eligible for payout request");
        }
    }

    private boolean isPayoutEligible(TransportPayment payment) {
        if (payment.getMethod() == PaymentMethod.TRANSFER) {
            if (payment.getPgTid() == null || payment.getPgTid().isBlank()) {
                log.info("skip payout for record-only transfer payment. orderId={}", payment.getOrderId());
                return false;
            }
        }
        return true;
    }

    private String defaultFailureReason(String failReason, String gatewayStatus) {
        if (failReason != null && !failReason.isBlank()) {
            return failReason;
        }
        if (gatewayStatus != null && !gatewayStatus.isBlank()) {
            return "gateway status=" + gatewayStatus;
        }
        return "unknown payout failure";
    }
}

