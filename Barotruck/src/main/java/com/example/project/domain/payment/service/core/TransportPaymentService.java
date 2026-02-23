package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentMethod;
import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.TossConfirmRequest;
import com.example.project.domain.payment.dto.paymentRequest.TossPrepareRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.dto.paymentResponse.TossPrepareResponse;
import com.example.project.member.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransportPaymentService {

    private final PaymentLifecycleService paymentLifecycleService;
    private final PaymentDisputeService paymentDisputeService;
    private final TossPaymentService tossPaymentService;

    public TransportPayment markPaid(
            Users currentUser,
            Long orderId,
            PaymentMethod method,
            String proofUrl,
            LocalDateTime paidAt
    ) {
        return paymentLifecycleService.markPaid(currentUser, orderId, method, proofUrl, paidAt);
    }

    public TransportPayment externalPay(
            Users currentUser,
            Long orderId,
            PaymentMethod method
    ) {
        return paymentLifecycleService.externalPay(currentUser, orderId, method);
    }

    public TransportPayment confirm(Users currentUser, Long orderId) {
        return paymentLifecycleService.confirmByDriver(currentUser, orderId);
    }

    public TransportPayment dispute(Users currentUser, Long orderId) {
        return paymentDisputeService.legacyDispute(currentUser, orderId);
    }

    public PaymentDispute createDispute(
            Users currentUser,
            Long orderId,
            CreatePaymentDisputeRequest request
    ) {
        return paymentDisputeService.createDispute(currentUser, orderId, request);
    }

    public PaymentDispute updateDisputeStatus(
            Users currentUser,
            Long orderId,
            Long disputeId,
            UpdatePaymentDisputeStatusRequest request
    ) {
        return paymentDisputeService.updateDisputeStatus(currentUser, orderId, disputeId, request);
    }

    public TossPrepareResponse prepareTossPayment(
            Users currentUser,
            Long orderId,
            TossPrepareRequest request
    ) {
        return tossPaymentService.prepare(currentUser, orderId, request);
    }

    public TransportPayment confirmTossPayment(
            Users currentUser,
            Long orderId,
            TossConfirmRequest request
    ) {
        return tossPaymentService.confirm(currentUser, orderId, request);
    }

    public void handleTossWebhook(String eventId, String payload) {
        tossPaymentService.handleWebhook(eventId, payload);
    }
}

