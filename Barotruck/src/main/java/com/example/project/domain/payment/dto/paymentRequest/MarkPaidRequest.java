// src/main/java/com/example/project/domain/payment/dto/MarkPaidRequest.java
package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MarkPaidRequest {
    private PaymentMethod method;
    private PaymentTiming paymentTiming;
    private String proofUrl;
    private LocalDateTime paidAt;
}

