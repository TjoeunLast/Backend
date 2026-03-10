package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentGatewayWebhookEventRepository extends JpaRepository<PaymentGatewayWebhookEvent, Long> {
    List<PaymentGatewayWebhookEvent> findByProviderAndEventTypeContainingIgnoreCaseAndPayloadContainingIgnoreCaseOrderByReceivedAtDesc(
            PaymentProvider provider,
            String eventType,
            String payloadKeyword,
            Pageable pageable
    );
}
