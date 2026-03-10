package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.BillingAgreementStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ShipperBillingAgreementResponse(
        Long agreementId,
        Long shipperUserId,
        PaymentProvider provider,
        PaymentMethod method,
        BillingAgreementStatus status,
        String customerKey,
        String billingKeyMasked,
        String cardCompany,
        String cardNumberMasked,
        String cardType,
        String ownerType,
        boolean active,
        LocalDateTime authenticatedAt,
        LocalDateTime lastChargedAt,
        LocalDateTime deactivatedAt,
        String deactivationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ShipperBillingAgreementResponse from(ShipperBillingAgreement agreement) {
        if (agreement == null) {
            return null;
        }
        return ShipperBillingAgreementResponse.builder()
                .agreementId(agreement.getAgreementId())
                .shipperUserId(agreement.getShipperUserId())
                .provider(agreement.getProvider())
                .method(agreement.getMethod())
                .status(agreement.getStatus())
                .customerKey(agreement.getCustomerKey())
                .billingKeyMasked(maskBillingKey(agreement.getBillingKey()))
                .cardCompany(agreement.getCardCompany())
                .cardNumberMasked(agreement.getCardNumberMasked())
                .cardType(agreement.getCardType())
                .ownerType(agreement.getOwnerType())
                .active(agreement.isActive())
                .authenticatedAt(agreement.getAuthenticatedAt())
                .lastChargedAt(agreement.getLastChargedAt())
                .deactivatedAt(agreement.getDeactivatedAt())
                .deactivationReason(agreement.getDeactivationReason())
                .createdAt(agreement.getCreatedAt())
                .updatedAt(agreement.getUpdatedAt())
                .build();
    }

    private static String maskBillingKey(String billingKey) {
        if (billingKey == null || billingKey.length() < 8) {
            return billingKey;
        }
        return billingKey.substring(0, 4) + "..." + billingKey.substring(billingKey.length() - 4);
    }
}
