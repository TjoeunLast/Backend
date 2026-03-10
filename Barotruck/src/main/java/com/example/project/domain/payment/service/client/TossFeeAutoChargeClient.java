package com.example.project.domain.payment.service.client;

import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.BillingAgreementStatus;
import com.example.project.domain.payment.repository.ShipperBillingAgreementRepository;
import com.example.project.global.toss.client.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.fee-auto-charge.mock-enabled", havingValue = "false", matchIfMissing = true)
public class TossFeeAutoChargeClient implements FeeAutoChargeClient {

    private final TossPaymentClient tossPaymentClient;
    private final ShipperBillingAgreementRepository billingAgreementRepository;

    @Override
    @Transactional
    public ChargeResult charge(Long invoiceId, Long shipperUserId, BigDecimal totalFee) {
        if (invoiceId == null || shipperUserId == null) {
            return new ChargeResult(false, "INVALID_INVOICE", "invoiceId and shipperUserId are required", null, null, null, null);
        }
        if (totalFee == null || totalFee.compareTo(BigDecimal.ZERO) <= 0) {
            return new ChargeResult(false, "INVALID_AMOUNT", "totalFee must be positive", null, null, null, null);
        }

        ShipperBillingAgreement agreement = billingAgreementRepository
                .findTopByShipperUserIdAndStatusOrderByAgreementIdDesc(shipperUserId, BillingAgreementStatus.ACTIVE)
                .orElse(null);
        if (agreement == null || !agreement.isActive()) {
            return new ChargeResult(false, "BILLING_NOT_REGISTERED", "active billing agreement not found", null, null, null, null);
        }
        if (isBlank(agreement.getBillingKey())) {
            agreement.deactivate("missing billingKey on active agreement", BillingAgreementStatus.DELETED);
            billingAgreementRepository.save(agreement);
            return new ChargeResult(false, "INVALID_BILLING_KEY", "billingKey is missing on active agreement", null, null, null, null);
        }
        if (isBlank(agreement.getCustomerKey())) {
            return new ChargeResult(false, "INVALID_CUSTOMER_KEY", "customerKey is missing on active agreement", null, null, null, null);
        }

        String orderId = createInvoiceOrderId(invoiceId, shipperUserId);
        String orderName = "Barotruck fee invoice " + YearMonth.now();
        TossPaymentClient.BillingChargeResult result = tossPaymentClient.chargeBillingKey(
                agreement.getBillingKey(),
                agreement.getCustomerKey(),
                orderId,
                orderName,
                totalFee
        );

        if (!result.success()) {
            if (result.hasInvalidBillingKey()) {
                agreement.deactivate(defaultIfBlank(result.failureSummary(), "invalid billing key"), BillingAgreementStatus.DELETED);
                billingAgreementRepository.save(agreement);
            }
            return new ChargeResult(
                    false,
                    result.failCode(),
                    defaultIfBlank(result.failMessage(), result.failureSummary()),
                    result.orderId(),
                    result.paymentKey(),
                    result.transactionId(),
                    result.rawPayload()
            );
        }

        agreement.markChargeSuccess();
        billingAgreementRepository.save(agreement);
        return new ChargeResult(
                true,
                null,
                null,
                result.orderId(),
                result.paymentKey(),
                result.transactionId(),
                result.rawPayload()
        );
    }

    private String createInvoiceOrderId(Long invoiceId, Long shipperUserId) {
        return "FI-" + Long.toString(shipperUserId, 36).toUpperCase() + "-" + Long.toString(invoiceId, 36).toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}
