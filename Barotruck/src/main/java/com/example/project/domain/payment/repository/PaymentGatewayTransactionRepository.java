package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentGatewayTransactionRepository extends JpaRepository<PaymentGatewayTransaction, Long> {
    Optional<PaymentGatewayTransaction> findByProviderAndPgOrderId(PaymentProvider provider, String pgOrderId);

    Optional<PaymentGatewayTransaction> findByProviderAndPaymentKey(PaymentProvider provider, String paymentKey);

    Optional<PaymentGatewayTransaction> findTopByOrderIdAndProviderOrderByCreatedAtDesc(Long orderId, PaymentProvider provider);

    List<PaymentGatewayTransaction> findAllByProviderAndStatus(PaymentProvider provider, GatewayTxStatus status);

    List<PaymentGatewayTransaction> findTop100ByProviderAndStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            PaymentProvider provider,
            GatewayTxStatus status,
            LocalDateTime nextRetryAt
    );

    List<PaymentGatewayTransaction> findTop100ByProviderAndStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentProvider provider,
            GatewayTxStatus status,
            LocalDateTime expiresAt
    );

    long countByProviderAndStatus(PaymentProvider provider, GatewayTxStatus status);

    long countByProviderAndStatusAndNextRetryAtLessThanEqual(
            PaymentProvider provider,
            GatewayTxStatus status,
            LocalDateTime nextRetryAt
    );

    long countByProviderAndStatusAndExpiresAtBefore(
            PaymentProvider provider,
            GatewayTxStatus status,
            LocalDateTime expiresAt
    );
}

