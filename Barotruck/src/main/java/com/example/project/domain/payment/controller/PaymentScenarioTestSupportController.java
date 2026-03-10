package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentResponse.PaymentScenarioSnapshotResponse;
import com.example.project.domain.payment.service.query.PaymentScenarioTestSupportService;
import com.example.project.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/payment-scenarios")
@ConditionalOnProperty(name = "payment.test-support.enabled", havingValue = "true")
public class PaymentScenarioTestSupportController {

    private final PaymentScenarioTestSupportService paymentScenarioTestSupportService;

    @GetMapping("/orders/{orderId}/snapshot")
    public ApiResponse<PaymentScenarioSnapshotResponse> getOrderSnapshot(
            @PathVariable("orderId") Long orderId
    ) {
        return ApiResponse.ok(paymentScenarioTestSupportService.getOrderSnapshot(orderId));
    }
}
