package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.TransportPaymentStatus;
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

    private final DriverPayoutItemRepository itemRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverPayoutGatewayClient payoutGatewayClient;
    private final EntityManager entityManager;

    @Scheduled(cron = "${payment.payout.cron:0 0 4 * * *}")
    @Transactional
    public void scheduleDailyPayout() {
        runPayoutForDate(LocalDate.now());
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
            if (payment.getDriverUserId() == null || itemRepository.existsByOrderId(payment.getOrderId())) {
                continue;
            }

            DriverPayoutItem item = itemRepository.save(
                    DriverPayoutItem.ready(batch, payment.getOrderId(), payment.getDriverUserId(), payment.getNetAmountSnapshot())
            );
            processItem(item);
        }

        long failedCount = itemRepository.countByBatch_BatchIdAndStatus(batch.getBatchId(), PayoutStatus.FAILED);
        if (failedCount == 0) {
            batch.markCompleted();
        } else {
            batch.markFailed("failed items: " + failedCount);
        }
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
            item.markCompleted(result.payoutRef());
            log.info("driver payout completed. orderId={}, itemId={}", item.getOrderId(), item.getItemId());
        } else {
            item.markFailed(result.failReason());
            log.warn("driver payout failed. orderId={}, itemId={}, reason={}",
                    item.getOrderId(), item.getItemId(), result.failReason());
        }
        return itemRepository.save(item);
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
}

