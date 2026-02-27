package com.example.project.domain.payment.service.client;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DummyExternalPaymentClient implements ExternalPaymentClient {

    @Override
    public ExternalPayResult pay(String merchantOrderId, long amount, PaymentMethod method) {
        // TODO: 실제 PG 연동(WebClient/RestTemplate)으로 교체
        return new ExternalPayResult(true, "DUMMY-" + UUID.randomUUID(), null);
    }
}