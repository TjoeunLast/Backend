package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.member.domain.Users;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeInvoiceService {

    private final FeeInvoiceRepository feeInvoiceRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final EntityManager entityManager;

    @Transactional
    public FeeInvoice generateForShipper(Long shipperUserId, YearMonth period) {
        String periodStr = period.toString(); // YYYY-MM

        FeeInvoice existing = feeInvoiceRepository.findByShipperUserIdAndPeriod(shipperUserId, periodStr).orElse(null);
        if (existing != null) return existing;

        LocalDateTime from = period.atDay(1).atStartOfDay();
        LocalDateTime to = period.plusMonths(1).atDay(1).atStartOfDay();

        List<TransportPayment> payments = transportPaymentRepository.findAll(); // MVP 단순 구현
        BigDecimal total = BigDecimal.ZERO;

        FeeInvoice invoice = FeeInvoice.issue(
                shipperUserId,
                periodStr,
                BigDecimal.ZERO,
                period.atEndOfMonth().atTime(23, 59, 59)
        );
        invoice = feeInvoiceRepository.save(invoice);

        for (TransportPayment p : payments) {
            if (!shipperUserId.equals(p.getShipperUserId())) continue;
            if (p.getPaidAt() == null) continue;
            if (p.getPaidAt().isBefore(from) || !p.getPaidAt().isBefore(to)) continue;

            // 카드 결제 건은 화주 수수료 인보이스 집계 대상에서 제외
            if (p.getMethod() == PaymentMethod.CARD) continue;

            if (p.getStatus() != TransportPaymentStatus.PAID && p.getStatus() != TransportPaymentStatus.CONFIRMED) continue;

            if (!existsInvoiceItem(invoice.getInvoiceId(), p.getOrderId())) {
                entityManager.persist(FeeInvoiceItem.of(invoice.getInvoiceId(), p.getOrderId(), p.getFeeAmountSnapshot()));
            }

            total = total.add(p.getFeeAmountSnapshot());
        }

        invoice.setTotalFee(total);
        return feeInvoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public FeeInvoice getMyInvoice(Users currentUser, YearMonth period) {
        Long shipperUserId = currentUser.getUserId();
        return feeInvoiceRepository.findByShipperUserIdAndPeriod(shipperUserId, period.toString())
                .orElseThrow(() -> new IllegalArgumentException("invoice not found"));
    }

    @Transactional
    public FeeInvoice markInvoicePaid(Users currentUser, Long invoiceId) {
        Long shipperUserId = currentUser.getUserId();

        FeeInvoice invoice = feeInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("invoice not found"));

        if (!shipperUserId.equals(invoice.getShipperUserId())) {
            throw new IllegalStateException("only owner can pay invoice");
        }

        invoice.markPaid();
        return feeInvoiceRepository.save(invoice);
    }

    private boolean existsInvoiceItem(Long invoiceId, Long orderId) {
        Long count = entityManager.createQuery(
                        "select count(i) from FeeInvoiceItem i where i.invoiceId = :invoiceId and i.orderId = :orderId",
                        Long.class
                )
                .setParameter("invoiceId", invoiceId)
                .setParameter("orderId", orderId)
                .getSingleResult();
        return count != null && count > 0;
    }
}