package com.example.project.domain.payment.service;

import com.example.project.domain.payment.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DummyExternalPaymentClient implements ExternalPaymentClient {

    @Override
    public ExternalPayResult pay(String merchantOrderId, long amount, PaymentMethod method) {
        // TODO: 나중에 WebClient/RestTemplate로 실제 결제사 API 호출로 교체
        return new ExternalPayResult(true, "DUMMY-" + UUID.randomUUID(), null);
    }
}
