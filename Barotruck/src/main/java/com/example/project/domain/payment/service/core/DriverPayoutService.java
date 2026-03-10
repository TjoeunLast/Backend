package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverPayoutService {

    private final DriverPayoutItemRepository itemRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverPayoutGatewayClient payoutGatewayClient;
    private final SettlementRepository settlementRepository;
    private final EntityManager entityManager;

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
                item.markCompleted(item.getPayoutRef());
                markSettlementCompletedOnPayout(item.getOrderId());
            } else if (result.failed()) {
                item.markFailed(defaultFailureReason(result.failReason(), result.gatewayStatus()), false);
            } else {
                continue;
            }

            itemRepository.save(item);
            refreshBatchStatus(item.getBatch());
        }
    }

    @Transactional
    public DriverPayoutBatch runPayoutForDate(LocalDate payoutDate) {
        DriverPayoutBatch batch = findBatchByDate(payoutDate);
        if (batch == null) {
            batch = DriverPayoutBatch.start(payoutDate);
            entityManager.persist(batch);
        }

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

            DriverPayoutItem item = itemRepository.save(
                    DriverPayoutItem.ready(batch, payment.getOrderId(), payment.getDriverUserId(), payment.getNetAmountSnapshot())
            );
            processItem(item);
        }
        refreshBatchStatus(batch);
        return batch;
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
                item.markCompleted(result.payoutRef());
                markSettlementCompletedOnPayout(item.getOrderId());
                log.info("driver payout completed. orderId={}, itemId={}", item.getOrderId(), item.getItemId());
            } else {
                item.markAccepted(result.payoutRef());
                log.info(
                        "driver payout requested. orderId={}, itemId={}, payoutRef={}, status={}",
                        item.getOrderId(),
                        item.getItemId(),
                        result.payoutRef(),
                        result.gatewayStatus()
                );
            }
        } else {
            item.markFailed(result.failReason());
            log.warn("driver payout failed. orderId={}, itemId={}, reason={}",
                    item.getOrderId(), item.getItemId(), result.failReason());
        }
        DriverPayoutItem savedItem = itemRepository.save(item);
        refreshBatchStatus(item.getBatch());
        return savedItem;
    }

    private DriverPayoutBatch findBatchByDate(LocalDate payoutDate) {
        var result = entityManager.createQuery(
                        "select b from DriverPayoutBatch b where b.batchDate = :batchDate",
                        DriverPayoutBatch.class
                )
                .setParameter("batchDate", payoutDate)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
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

    private void refreshBatchStatus(DriverPayoutBatch batch) {
        if (batch == null || batch.getBatchId() == null) {
            return;
        }

        long totalCount = itemRepository.countByBatch_BatchId(batch.getBatchId());
        long requestedCount = itemRepository.countByBatch_BatchIdAndStatus(batch.getBatchId(), PayoutStatus.REQUESTED);
        long failedCount = itemRepository.countByBatch_BatchIdAndStatus(batch.getBatchId(), PayoutStatus.FAILED);

        if (totalCount == 0) {
            batch.markCompleted();
            return;
        }

        if (requestedCount > 0) {
            return;
        }

        if (failedCount > 0) {
            batch.markFailed("failed items: " + failedCount);
            return;
        }

        batch.markCompleted();
    }

    private void markSettlementCompletedOnPayout(Long orderId) {
        settlementRepository.findByOrderId(orderId).ifPresentOrElse(settlement -> {
            settlement.setStatus(SettlementStatus.COMPLETED);
            if (settlement.getFeeCompleteDate() == null) {
                settlement.setFeeCompleteDate(LocalDateTime.now());
            }
            settlementRepository.save(settlement);
        }, () -> log.warn("payout completed but settlement not found. orderId={}", orderId));
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

