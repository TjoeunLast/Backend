package com.example.project.domain.payment.dto.paymentRequest;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateTransportPaymentStatusRequest {
    private TransportPaymentStatus status;
    private PaymentMethod method;
    private PaymentTiming paymentTiming;
    private String proofUrl;
    private String adminMemo;
    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;
}
