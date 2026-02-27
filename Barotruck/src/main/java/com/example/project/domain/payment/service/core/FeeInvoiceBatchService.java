package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeInvoiceStatus;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.FeeAutoChargeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeInvoiceBatchService {

    private final FeeInvoiceService feeInvoiceService;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final FeeAutoChargeClient feeAutoChargeClient;

    @Scheduled(cron = "${payment.fee-invoice.generate.cron:0 5 1 1 * *}")
    @Transactional
    public void scheduleGeneratePreviousMonthInvoices() {
        runInvoiceGeneration(YearMonth.now().minusMonths(1));
    }

    @Scheduled(cron = "${payment.fee-invoice.auto-charge.cron:0 20 1 * * *}")
    @Transactional
    public void scheduleAutoChargeInvoices() {
        runAutoCharge();
    }

    @Transactional
    public int runInvoiceGeneration(YearMonth period) {
        LocalDateTime from = period.atDay(1).atStartOfDay();
        LocalDateTime to = period.plusMonths(1).atDay(1).atStartOfDay();
        List<Long> shipperUserIds = transportPaymentRepository.findDistinctShipperUserIdByPaidAtBetween(from, to);

        int generated = 0;
        for (Long shipperUserId : shipperUserIds) {
            feeInvoiceService.generateForShipper(shipperUserId, period);
            generated++;
        }
        log.info("fee invoice generation done. period={}, shipperCount={}", period, generated);
        return generated;
    }

    @Transactional
    public int runAutoCharge() {
        List<FeeInvoice> targets = new ArrayList<>();
        targets.addAll(feeInvoiceRepository.findAllByStatus(FeeInvoiceStatus.ISSUED));
        targets.addAll(feeInvoiceRepository.findAllByStatus(FeeInvoiceStatus.OVERDUE));

        int paidCount = 0;
        for (FeeInvoice invoice : targets) {
            var chargeResult = feeAutoChargeClient.charge(invoice.getShipperUserId(), invoice.getTotalFee());
            if (chargeResult.success()) {
                invoice.markPaid();
                paidCount++;
            } else {
                invoice.markOverdue();
            }
            feeInvoiceRepository.save(invoice);
        }
        log.info("fee invoice auto charge done. total={}, paid={}", targets.size(), paidCount);
        return paidCount;
    }
}


