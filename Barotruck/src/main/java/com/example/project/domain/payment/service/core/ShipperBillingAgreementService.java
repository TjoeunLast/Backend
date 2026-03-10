package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.BillingAgreementStatus;
import com.example.project.domain.payment.dto.paymentRequest.TossBillingIssueRequest;
import com.example.project.domain.payment.dto.paymentResponse.ShipperBillingAgreementResponse;
import com.example.project.domain.payment.dto.paymentResponse.TossBillingContextResponse;
import com.example.project.domain.payment.repository.ShipperBillingAgreementRepository;
import com.example.project.global.toss.client.TossPaymentClient;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipperBillingAgreementService {

    private final ShipperBillingAgreementRepository billingAgreementRepository;
    private final TossPaymentClient tossPaymentClient;

    @Value("${payment.toss.client-key:}")
    private String tossClientKey;

    @Value("${payment.toss.billing.redirect.success-url:${payment.toss.redirect.success-url:barotruck://billing/success}}")
    private String billingSuccessUrl;

    @Value("${payment.toss.billing.redirect.fail-url:${payment.toss.redirect.fail-url:barotruck://billing/fail}}")
    private String billingFailUrl;

    @Transactional(readOnly = true)
    public TossBillingContextResponse getBillingContext(Users currentUser) {
        requireShipper(currentUser);
        return TossBillingContextResponse.builder()
                .clientKey(normalize(tossClientKey))
                .customerKey(resolveCustomerKey(currentUser.getUserId()))
                .successUrl(normalize(billingSuccessUrl))
                .failUrl(normalize(billingFailUrl))
                .build();
    }

    @Transactional
    public ShipperBillingAgreementResponse issueBillingAgreement(Users currentUser, TossBillingIssueRequest request) {
        requireShipper(currentUser);
        if (request == null || isBlank(request.getAuthKey())) {
            throw new IllegalArgumentException("authKey is required");
        }

        String expectedCustomerKey = resolveCustomerKey(currentUser.getUserId());
        String customerKey = isBlank(request.getCustomerKey()) ? expectedCustomerKey : request.getCustomerKey().trim();
        if (!expectedCustomerKey.equals(customerKey)) {
            throw new IllegalArgumentException("customerKey mismatch");
        }

        TossPaymentClient.BillingIssueResult result = tossPaymentClient.issueBillingKey(request.getAuthKey(), customerKey);
        if (!result.success()) {
            throw new IllegalStateException("toss billing issue failed: " + defaultIfBlank(result.failureSummary(), "unknown"));
        }
        String resolvedCustomerKey = normalize(result.customerKey());
        if (resolvedCustomerKey != null && !customerKey.equals(resolvedCustomerKey)) {
            throw new IllegalStateException("customerKey mismatch in toss billing issue response");
        }
        if (isBlank(result.billingKey())) {
            throw new IllegalStateException("billingKey missing from toss response");
        }

        deactivateActiveAgreements(currentUser.getUserId(), "replaced by new billing agreement");

        ShipperBillingAgreement agreement = billingAgreementRepository.findByCustomerKey(customerKey).orElse(null);
        if (agreement == null) {
            agreement = ShipperBillingAgreement.activate(
                    currentUser.getUserId(),
                    customerKey,
                    result.billingKey(),
                    result.cardCompany(),
                    result.cardNumberMasked(),
                    result.cardType(),
                    result.ownerType()
            );
        } else {
            agreement.refresh(
                    result.billingKey(),
                    result.cardCompany(),
                    result.cardNumberMasked(),
                    result.cardType(),
                    result.ownerType()
            );
        }

        return ShipperBillingAgreementResponse.from(billingAgreementRepository.save(agreement));
    }

    @Transactional(readOnly = true)
    public ShipperBillingAgreementResponse getMyAgreement(Users currentUser) {
        requireShipper(currentUser);
        return ShipperBillingAgreementResponse.from(
                billingAgreementRepository
                        .findTopByShipperUserIdOrderByAgreementIdDesc(currentUser.getUserId())
                        .orElse(null)
        );
    }

    @Transactional
    public ShipperBillingAgreementResponse deactivateMyAgreement(Users currentUser) {
        requireShipper(currentUser);
        ShipperBillingAgreement agreement = billingAgreementRepository
                .findTopByShipperUserIdAndStatusOrderByAgreementIdDesc(currentUser.getUserId(), BillingAgreementStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("active billing agreement not found"));

        if (isBlank(agreement.getBillingKey())) {
            agreement.deactivate("missing billingKey on active agreement", BillingAgreementStatus.DELETED);
            return ShipperBillingAgreementResponse.from(billingAgreementRepository.save(agreement));
        }
        if (isBlank(agreement.getCustomerKey())) {
            agreement.deactivate("missing customerKey on active agreement", BillingAgreementStatus.DELETED);
            return ShipperBillingAgreementResponse.from(billingAgreementRepository.save(agreement));
        }

        TossPaymentClient.BillingDeleteResult result = tossPaymentClient.deleteBillingKey(
                agreement.getBillingKey(),
                agreement.getCustomerKey()
        );
        if (!result.success()) {
            if (result.hasInvalidBillingKey()) {
                agreement.deactivate(defaultIfBlank(result.failureSummary(), "invalid billing key"), BillingAgreementStatus.DELETED);
                return ShipperBillingAgreementResponse.from(billingAgreementRepository.save(agreement));
            }
            throw new IllegalStateException("toss billing delete failed: " + defaultIfBlank(result.failureSummary(), "unknown"));
        }

        agreement.deactivate("shipper deactivated billing agreement", BillingAgreementStatus.INACTIVE);
        return ShipperBillingAgreementResponse.from(billingAgreementRepository.save(agreement));
    }

    @Transactional(readOnly = true)
    public ShipperBillingAgreementResponse getAgreementByShipperUserId(Long shipperUserId) {
        return ShipperBillingAgreementResponse.from(
                billingAgreementRepository.findTopByShipperUserIdOrderByAgreementIdDesc(shipperUserId).orElse(null)
        );
    }

    private void deactivateActiveAgreements(Long shipperUserId, String reason) {
        billingAgreementRepository.findTopByShipperUserIdAndStatusOrderByAgreementIdDesc(
                shipperUserId,
                BillingAgreementStatus.ACTIVE
        ).ifPresent(agreement -> {
            agreement.deactivate(reason, BillingAgreementStatus.INACTIVE);
            billingAgreementRepository.save(agreement);
        });
    }

    private String resolveCustomerKey(Long shipperUserId) {
        return billingAgreementRepository.findTopByShipperUserIdOrderByAgreementIdDesc(shipperUserId)
                .map(ShipperBillingAgreement::getCustomerKey)
                .orElseGet(() -> "SHIPPER-" + Long.toString(shipperUserId, 36).toUpperCase());
    }

    private void requireShipper(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can manage billing agreement");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}
