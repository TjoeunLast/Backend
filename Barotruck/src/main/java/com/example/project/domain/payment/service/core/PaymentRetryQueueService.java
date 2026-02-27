package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.global.toss.client.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryQueueService {

    private final PaymentGatewayTransactionRepository transactionRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentLifecycleService paymentLifecycleService;

    @Value("${payment.toss.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Scheduled(cron = "${payment.toss.expire.cron:0 */5 * * * *}")
    @Transactional
    public void scheduleExpirePreparedTransactions() {
        expirePreparedTransactions();
    }

    @Scheduled(cron = "${payment.toss.retry.cron:0 */5 * * * *}")
    @Transactional
    public void scheduleRetryFailedTransactions() {
        processFailedRetryQueue();
    }

    @Transactional
    public int expirePreparedTransactions() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentGatewayTransaction> expiredPrepared = transactionRepository
                .findTop100ByProviderAndStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentProvider.TOSS,
                        GatewayTxStatus.PREPARED,
                        now
                );

        int expiredCount = 0;
        for (PaymentGatewayTransaction tx : expiredPrepared) {
            if (!tx.isExpired(now)) {
                continue;
            }
            tx.markExpired();
            transactionRepository.save(tx);
            expiredCount++;
        }

        if (expiredCount > 0) {
            log.info("expired prepared toss transactions: {}", expiredCount);
        }
        return expiredCount;
    }

    @Transactional
    public int processFailedRetryQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentGatewayTransaction> candidates = transactionRepository
                .findTop100ByProviderAndStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        PaymentProvider.TOSS,
                        GatewayTxStatus.FAILED,
                        now
                );

        int retried = 0;
        int recovered = 0;
        for (PaymentGatewayTransaction tx : candidates) {
            if (!tx.canRetry(now, maxRetryAttempts)) {
                if (shouldStopRetry(tx)) {
                    tx.stopRetry(
                            "RETRY_STOPPED",
                            "retry stopped: missing paymentKey or max retry exceeded"
                    );
                    transactionRepository.save(tx);
                }
                continue;
            }

            retried++;
            TossPaymentClient.ConfirmResult result =
                    tossPaymentClient.confirm(tx.getPaymentKey(), tx.getPgOrderId(), tx.getAmount());

            if (result.success()) {
                tx.markConfirmed(tx.getPaymentKey(), result.transactionId(), result.rawPayload());
                transactionRepository.save(tx);
                try {
                    paymentLifecycleService.applyPaidFromGatewayTx(tx);
                    recovered++;
                } catch (Exception e) {
                    tx.markFailed(
                            "INTERNAL_APPLY_ERROR",
                            trimLength(defaultIfBlank(e.getMessage(), "internal apply failed"), 500),
                            result.rawPayload(),
                            true
                    );
                    transactionRepository.save(tx);
                }
                continue;
            }

            tx.markFailed(
                    result.failCode(),
                    result.failMessage(),
                    result.rawPayload(),
                    isRetryableFailure(result.failCode(), result.failMessage())
            );
            transactionRepository.save(tx);
        }

        if (retried > 0) {
            log.info("processed toss retry queue. retried={}, recovered={}", retried, recovered);
        }
        return recovered;
    }

    private boolean shouldStopRetry(PaymentGatewayTransaction tx) {
        if (tx.getNextRetryAt() == null) {
            return false;
        }
        if (tx.getPaymentKey() == null || tx.getPaymentKey().isBlank()) {
            return true;
        }
        int retryCount = tx.getRetryCount() == null ? 0 : tx.getRetryCount();
        return retryCount >= maxRetryAttempts;
    }

    private boolean isRetryableFailure(String failCode, String failMessage) {
        String code = defaultIfBlank(failCode, "").toUpperCase(Locale.ROOT);
        String message = defaultIfBlank(failMessage, "").toUpperCase(Locale.ROOT);

        if (code.contains("INVALID") || code.contains("UNAUTHORIZED")) {
            return false;
        }
        if (code.contains("AMOUNT_MISMATCH") || message.contains("AMOUNT")) {
            return false;
        }
        if (code.contains("EXPIRED")) {
            return false;
        }
        return true;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String trimLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
