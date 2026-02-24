package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.paymentEnum.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.TransportPaymentStatus;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentGatewayTransactionRepository transactionRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentLifecycleService paymentLifecycleService;

    @Scheduled(cron = "${payment.reconciliation.cron:0 10 2 * * *}")
    public void runDailyReconciliation() {
        var confirmedGatewayTxs =
                transactionRepository.findAllByProviderAndStatus(PaymentProvider.TOSS, GatewayTxStatus.CONFIRMED);

        int mismatchCount = 0;
        int recoveredCount = 0;
        for (var tx : confirmedGatewayTxs) {
            TransportPayment payment = transportPaymentRepository.findByOrderId(tx.getOrderId()).orElse(null);
            if (payment != null && isPaidOrSettledState(payment.getStatus())) {
                continue;
            }

            mismatchCount++;
            try {
                paymentLifecycleService.applyPaidFromGatewayTx(tx);
                recoveredCount++;
            } catch (Exception e) {
                log.warn("reconciliation recover failed. orderId={}, txId={}, reason={}",
                        tx.getOrderId(), tx.getTxId(), e.getMessage());
            }
        }

        int unresolved = mismatchCount - recoveredCount;
        if (unresolved > 0) {
            log.warn("payment reconciliation finished with unresolved mismatch. totalConfirmed={}, mismatch={}, recovered={}, unresolved={}",
                    confirmedGatewayTxs.size(), mismatchCount, recoveredCount, unresolved);
            return;
        }
        log.info("payment reconciliation ok. totalConfirmed={}, mismatch={}, recovered={}",
                confirmedGatewayTxs.size(), mismatchCount, recoveredCount);
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
}


