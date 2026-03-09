package com.example.project.domain.payment.service.query;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutBatchStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeInvoiceStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.GatewayTransactionStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentDisputeStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentReconciliationStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentRetryQueueStatusResponse;
import com.example.project.domain.payment.repository.DriverPayoutBatchRepository;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentStatusQueryService {

    private final PaymentDisputeRepository paymentDisputeRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final DriverPayoutBatchRepository driverPayoutBatchRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final TransportPaymentRepository transportPaymentRepository;

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
        return FeeInvoiceStatusResponse.from(invoice);
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
        return DriverPayoutItemStatusResponse.from(item);
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
}
