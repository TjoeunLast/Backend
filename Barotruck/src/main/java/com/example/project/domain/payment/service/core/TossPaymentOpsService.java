package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentRequest.CancelTossPaymentRequest;
import com.example.project.domain.payment.dto.paymentResponse.GatewayTransactionStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPaymentComparisonResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossPaymentLookupResponse;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.global.toss.client.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TossPaymentOpsService {

    private final TossPaymentClient tossPaymentClient;
    private final PaymentGatewayTransactionRepository transactionRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentLifecycleService paymentLifecycleService;
    private final DriverPayoutItemRepository payoutItemRepository;

    @Transactional(readOnly = true)
    public TossPaymentLookupResponse lookupByPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("paymentKey is required");
        }
        TossPaymentClient.LookupResult result = tossPaymentClient.lookupByPaymentKey(paymentKey);
        return toLookupResponse(result);
    }

    @Transactional(readOnly = true)
    public TossPaymentComparisonResponse lookupByOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        PaymentGatewayTransaction tx = transactionRepository
                .findTopByOrderIdAndProviderOrderByCreatedAtDesc(orderId, PaymentProvider.TOSS)
                .orElseThrow(() -> new IllegalArgumentException("toss transaction not found. orderId=" + orderId));

        TossPaymentClient.LookupResult result;
        if (tx.getPaymentKey() != null && !tx.getPaymentKey().isBlank()) {
            result = tossPaymentClient.lookupByPaymentKey(tx.getPaymentKey());
        } else {
            result = tossPaymentClient.lookupByOrderId(tx.getPgOrderId());
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId).orElse(null);
        TossPaymentLookupResponse lookupResponse = toLookupResponse(result);
        boolean mismatch = isMismatch(tx, payment, lookupResponse);
        return TossPaymentComparisonResponse.of(
                GatewayTransactionStatusResponse.from(tx),
                payment,
                lookupResponse,
                mismatch,
                resolveMismatchReason(tx, payment, lookupResponse)
        );
    }

    @Transactional
    public TransportPayment cancelOrderPayment(Long orderId, CancelTossPaymentRequest request) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        PaymentGatewayTransaction tx = transactionRepository
                .findTopByOrderIdAndProviderOrderByCreatedAtDesc(orderId, PaymentProvider.TOSS)
                .orElseThrow(() -> new IllegalArgumentException("toss transaction not found. orderId=" + orderId));
        if (tx.getStatus() == GatewayTxStatus.CANCELED) {
            return transportPaymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));
        }
        if (tx.getStatus() != GatewayTxStatus.CONFIRMED) {
            throw new IllegalStateException("only confirmed toss transaction can be canceled");
        }
        if (tx.getPaymentKey() == null || tx.getPaymentKey().isBlank()) {
            throw new IllegalStateException("paymentKey not found for cancel");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));
        validateCancelable(payment, orderId);

        BigDecimal cancelAmount = request == null ? null : request.getCancelAmount();
        validateCancelAmount(cancelAmount, tx.getAmount());

        String cancelReason = request == null ? null : request.getCancelReason();
        TossPaymentClient.CancelResult result = tossPaymentClient.cancel(tx.getPaymentKey(), cancelReason, cancelAmount);
        if (!result.success()) {
            throw new IllegalStateException("toss cancel failed: " + defaultIfBlank(result.failureSummary(), "unknown"));
        }
        validateFullCancelResult(tx, result);

        BigDecimal resolvedCanceledAmount = firstNonNull(result.cancelAmount(), tx.getAmount());
        String effectiveCancelReason = defaultIfBlank(cancelReason, "admin cancel");

        tx.markCanceled(
                result.rawPayload(),
                effectiveCancelReason,
                resolvedCanceledAmount,
                result.canceledAt(),
                result.transactionId()
        );
        tx.applyGatewayStatus(defaultIfBlank(result.normalizedStatus(), GatewayTxStatus.CANCELED.name()));
        transactionRepository.save(tx);

        paymentLifecycleService.applyCanceledFromGatewayTx(tx);
        return transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found after cancel. orderId=" + orderId));
    }

    private void validateCancelable(TransportPayment payment, Long orderId) {
        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            return;
        }
        if (payment.getStatus() != TransportPaymentStatus.PAID) {
            throw new IllegalStateException("only PAID transport payment can be canceled safely");
        }
        if (payoutItemRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("cannot cancel payment after payout item exists");
        }
    }

    private TossPaymentLookupResponse toLookupResponse(TossPaymentClient.LookupResult result) {
        if (result == null) {
            return null;
        }
        String status = result.success()
                ? result.status()
                : defaultIfBlank(result.normalizedStatus(), "LOOKUP_FAILED");
        return TossPaymentLookupResponse.builder()
                .paymentKey(result.paymentKey())
                .orderId(result.orderId())
                .status(status)
                .method(result.methodText())
                .easyPayProvider(result.easyPayProvider())
                .totalAmount(result.totalAmount())
                .suppliedAmount(result.suppliedAmount())
                .vat(result.vat())
                .approvedAt(result.approvedAt())
                .lastTransactionAt(result.lastTransactionAt())
                .cancels(result.cancels() == null ? List.of() : result.cancels().stream().map(cancel ->
                        TossPaymentLookupResponse.CancelHistory.builder()
                                .cancelAmount(cancel.cancelAmount())
                                .cancelReason(cancel.cancelReason())
                                .canceledAt(cancel.canceledAt())
                                .transactionKey(cancel.transactionKey())
                                .cancelStatus(cancel.status())
                                .build()
                ).toList())
                .rawPayload(defaultIfBlank(result.rawPayload(), result.failureSummary()))
                .build();
    }

    private boolean isMismatch(
            PaymentGatewayTransaction tx,
            TransportPayment payment,
            TossPaymentLookupResponse lookup
    ) {
        return resolveMismatchReason(tx, payment, lookup) != null;
    }

    private String resolveMismatchReason(
            PaymentGatewayTransaction tx,
            TransportPayment payment,
            TossPaymentLookupResponse lookup
    ) {
        if (lookup == null) {
            return "missing toss lookup response";
        }
        if (!isLookupSuccess(lookup)) {
            return defaultIfBlank(lookup.rawPayload(), "toss lookup failed");
        }
        if (lookup.status() == null) {
            return "missing toss gateway status";
        }
        if (tx.getPaymentKey() != null && lookup.paymentKey() != null && !tx.getPaymentKey().equals(lookup.paymentKey())) {
            return "paymentKey mismatch";
        }
        if (tx.getAmount() != null && lookup.totalAmount() != null && tx.getAmount().compareTo(lookup.totalAmount()) != 0) {
            return "amount mismatch";
        }
        if (payment != null && payment.getStatus() == TransportPaymentStatus.CANCELLED && !TossPaymentClient.isCanceledStatus(lookup.status())) {
            return "internal canceled but toss is not canceled";
        }
        if (payment != null && payment.getStatus() == TransportPaymentStatus.PAID && TossPaymentClient.isCanceledStatus(lookup.status())) {
            return "toss canceled but internal payment still paid";
        }
        return null;
    }

    private void validateCancelAmount(BigDecimal cancelAmount, BigDecimal originalAmount) {
        if (cancelAmount == null) {
            return;
        }
        if (cancelAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("cancelAmount must be positive");
        }
        if (originalAmount == null) {
            throw new IllegalStateException("original payment amount missing for cancel validation");
        }
        if (originalAmount.compareTo(cancelAmount) != 0) {
            throw new IllegalStateException("partial cancel is not supported in current payment model");
        }
    }

    private void validateFullCancelResult(PaymentGatewayTransaction tx, TossPaymentClient.CancelResult result) {
        if (result == null) {
            throw new IllegalStateException("toss cancel result is missing");
        }
        if (result.isPartialCancel(tx.getAmount())) {
            throw new IllegalStateException("toss partial cancel is not supported in current payment model");
        }
        String normalizedStatus = result.normalizedStatus();
        if (normalizedStatus != null && !TossPaymentClient.isCanceledStatus(normalizedStatus)) {
            throw new IllegalStateException("unexpected toss cancel status: " + normalizedStatus);
        }
    }

    private boolean isLookupSuccess(TossPaymentLookupResponse lookup) {
        return lookup.status() != null && !"LOOKUP_FAILED".equalsIgnoreCase(lookup.status());
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
