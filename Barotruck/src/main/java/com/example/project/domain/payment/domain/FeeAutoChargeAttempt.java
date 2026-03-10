package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeAutoChargeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "FEE_AUTO_CHARGE_ATTEMPTS",
        indexes = {
                @Index(name = "IDX_FEE_AUTO_CHARGE_INVOICE", columnList = "INVOICE_ID"),
                @Index(name = "IDX_FEE_AUTO_CHARGE_SHIPPER", columnList = "SHIPPER_USER_ID"),
                @Index(name = "IDX_FEE_AUTO_CHARGE_STATUS", columnList = "STATUS")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeAutoChargeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ATTEMPT_ID")
    private Long attemptId;

    @Column(name = "INVOICE_ID", nullable = false)
    private Long invoiceId;

    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    @Column(name = "AGREEMENT_ID")
    private Long agreementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private FeeAutoChargeStatus status;

    @Column(name = "ORDER_ID", length = 120)
    private String orderId;

    @Column(name = "PAYMENT_KEY", length = 200)
    private String paymentKey;

    @Column(name = "TRANSACTION_ID", length = 200)
    private String transactionId;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "FAIL_CODE", length = 100)
    private String failCode;

    @Column(name = "FAIL_REASON", length = 2000)
    private String failReason;

    @Lob
    @Column(name = "RAW_PAYLOAD")
    private String rawPayload;

    @Column(name = "ATTEMPTED_AT", nullable = false)
    private LocalDateTime attemptedAt;

    public static FeeAutoChargeAttempt success(
            Long invoiceId,
            Long shipperUserId,
            Long agreementId,
            String orderId,
            String paymentKey,
            String transactionId,
            BigDecimal amount,
            String rawPayload
    ) {
        return FeeAutoChargeAttempt.builder()
                .invoiceId(invoiceId)
                .shipperUserId(shipperUserId)
                .agreementId(agreementId)
                .provider(PaymentProvider.TOSS)
                .status(FeeAutoChargeStatus.SUCCEEDED)
                .orderId(orderId)
                .paymentKey(paymentKey)
                .transactionId(transactionId)
                .amount(amount)
                .rawPayload(rawPayload)
                .attemptedAt(LocalDateTime.now())
                .build();
    }

    public static FeeAutoChargeAttempt failed(
            Long invoiceId,
            Long shipperUserId,
            Long agreementId,
            String orderId,
            BigDecimal amount,
            String failCode,
            String failReason,
            String rawPayload
    ) {
        return FeeAutoChargeAttempt.builder()
                .invoiceId(invoiceId)
                .shipperUserId(shipperUserId)
                .agreementId(agreementId)
                .provider(PaymentProvider.TOSS)
                .status(FeeAutoChargeStatus.FAILED)
                .orderId(orderId)
                .amount(amount)
                .failCode(failCode)
                .failReason(failReason)
                .rawPayload(rawPayload)
                .attemptedAt(LocalDateTime.now())
                .build();
    }
}
