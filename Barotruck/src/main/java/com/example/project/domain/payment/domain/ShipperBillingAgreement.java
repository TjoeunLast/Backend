package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.BillingAgreementStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIPPER_BILLING_AGREEMENTS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SHIPPER_BILLING_CUSTOMER_KEY", columnNames = {"CUSTOMER_KEY"}),
                @UniqueConstraint(name = "UK_SHIPPER_BILLING_BILLING_KEY", columnNames = {"BILLING_KEY"})
        },
        indexes = {
                @Index(name = "IDX_SHIPPER_BILLING_SHIPPER", columnList = "SHIPPER_USER_ID"),
                @Index(name = "IDX_SHIPPER_BILLING_STATUS", columnList = "STATUS")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipperBillingAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AGREEMENT_ID")
    private Long agreementId;

    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "METHOD", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "CUSTOMER_KEY", nullable = false, length = 120)
    private String customerKey;

    @Column(name = "BILLING_KEY", nullable = false, length = 200)
    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private BillingAgreementStatus status;

    @Column(name = "CARD_COMPANY", length = 100)
    private String cardCompany;

    @Column(name = "CARD_NUMBER_MASKED", length = 50)
    private String cardNumberMasked;

    @Column(name = "CARD_TYPE", length = 50)
    private String cardType;

    @Column(name = "OWNER_TYPE", length = 50)
    private String ownerType;

    @Column(name = "AUTHENTICATED_AT", nullable = false)
    private LocalDateTime authenticatedAt;

    @Column(name = "LAST_CHARGED_AT")
    private LocalDateTime lastChargedAt;

    @Column(name = "DEACTIVATED_AT")
    private LocalDateTime deactivatedAt;

    @Column(name = "DEACTIVATION_REASON", length = 1000)
    private String deactivationReason;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = BillingAgreementStatus.ACTIVE;
        }
        if (this.provider == null) {
            this.provider = PaymentProvider.TOSS;
        }
        if (this.method == null) {
            this.method = PaymentMethod.CARD;
        }
        if (this.authenticatedAt == null) {
            this.authenticatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static ShipperBillingAgreement activate(
            Long shipperUserId,
            String customerKey,
            String billingKey,
            String cardCompany,
            String cardNumberMasked,
            String cardType,
            String ownerType
    ) {
        return ShipperBillingAgreement.builder()
                .shipperUserId(shipperUserId)
                .provider(PaymentProvider.TOSS)
                .method(PaymentMethod.CARD)
                .customerKey(customerKey)
                .billingKey(billingKey)
                .status(BillingAgreementStatus.ACTIVE)
                .cardCompany(cardCompany)
                .cardNumberMasked(cardNumberMasked)
                .cardType(cardType)
                .ownerType(ownerType)
                .authenticatedAt(LocalDateTime.now())
                .build();
    }

    public void refresh(
            String billingKey,
            String cardCompany,
            String cardNumberMasked,
            String cardType,
            String ownerType
    ) {
        this.billingKey = billingKey;
        this.cardCompany = cardCompany;
        this.cardNumberMasked = cardNumberMasked;
        this.cardType = cardType;
        this.ownerType = ownerType;
        this.status = BillingAgreementStatus.ACTIVE;
        this.deactivatedAt = null;
        this.deactivationReason = null;
        this.authenticatedAt = LocalDateTime.now();
    }

    public void markChargeSuccess() {
        this.lastChargedAt = LocalDateTime.now();
    }

    public void deactivate(String reason, BillingAgreementStatus nextStatus) {
        this.status = nextStatus == null ? BillingAgreementStatus.INACTIVE : nextStatus;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
    }

    public boolean isActive() {
        return this.status == BillingAgreementStatus.ACTIVE;
    }
}
