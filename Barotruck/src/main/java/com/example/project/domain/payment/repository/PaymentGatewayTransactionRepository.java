package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.paymentEnum.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.GatewayTxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentGatewayTransactionRepository extends JpaRepository<PaymentGatewayTransaction, Long> {
    Optional<PaymentGatewayTransaction> findByProviderAndPgOrderId(PaymentProvider provider, String pgOrderId);

    Optional<PaymentGatewayTransaction> findByProviderAndPaymentKey(PaymentProvider provider, String paymentKey);

    Optional<PaymentGatewayTransaction> findTopByOrderIdAndProviderOrderByCreatedAtDesc(Long orderId, PaymentProvider provider);

    List<PaymentGatewayTransaction> findAllByProviderAndStatus(PaymentProvider provider, GatewayTxStatus status);
}

