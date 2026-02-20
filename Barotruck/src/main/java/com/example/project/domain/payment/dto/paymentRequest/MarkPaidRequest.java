// src/main/java/com/example/project/domain/payment/dto/MarkPaidRequest.java
package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.PaymentMethod;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MarkPaidRequest {
    private PaymentMethod method;
    private String proofUrl;
    private LocalDateTime paidAt;
}
