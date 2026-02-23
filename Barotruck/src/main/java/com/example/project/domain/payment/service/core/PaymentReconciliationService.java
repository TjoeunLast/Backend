package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.paymentEnum.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.TransportPaymentStatus;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentGatewayTransactionRepository transactionRepository;
    private final TransportPaymentRepository transportPaymentRepository;

    @Scheduled(cron = "${payment.reconciliation.cron:0 10 2 * * *}")
    public void runDailyReconciliation() {
        long confirmedGatewayCount = transactionRepository
                .findAllByProviderAndStatus(PaymentProvider.TOSS, GatewayTxStatus.CONFIRMED)
                .size();

        long paidCount = transportPaymentRepository.findAllByStatusIn(List.of(
                TransportPaymentStatus.PAID,
                TransportPaymentStatus.CONFIRMED,
                TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
        )).size();

        if (confirmedGatewayCount != paidCount) {
            log.warn("payment reconciliation mismatch. gatewayConfirmed={}, internalPaidOrConfirmed={}",
                    confirmedGatewayCount, paidCount);
        } else {
            log.info("payment reconciliation ok. count={}", paidCount);
        }
    }
}


