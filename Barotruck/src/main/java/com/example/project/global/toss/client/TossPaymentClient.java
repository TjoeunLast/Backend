package com.example.project.global.toss.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TossPaymentClient {

    ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount);
    LookupResult lookupByPaymentKey(String paymentKey);
    LookupResult lookupByOrderId(String orderId);
    CancelResult cancel(String paymentKey, String cancelReason, BigDecimal cancelAmount);
    BillingIssueResult issueBillingKey(String authKey, String customerKey);
    BillingChargeResult chargeBillingKey(String billingKey, String customerKey, String orderId, String orderName, BigDecimal amount);
    BillingDeleteResult deleteBillingKey(String billingKey, String customerKey);

    static String summarizeFailure(String failCode, String failMessage) {
        String code = trimToNull(failCode);
        String message = trimToNull(failMessage);
        if (code != null && message != null) {
            return code + ": " + message;
        }
        return code != null ? code : message;
    }

    static boolean isCanceledStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized != null && normalized.contains("CANCEL");
    }

    static boolean isPartialCanceledStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized != null && normalized.contains("PARTIAL");
    }

    static boolean isInvalidBillingKeyFailure(String failCode, String failMessage) {
        String code = normalizeStatus(failCode);
        String rawMessage = trimToNull(failMessage);
        String upperMessage = rawMessage == null ? null : rawMessage.toUpperCase();
        String normalizedMessage = normalizeStatus(failMessage);
        if (code == null && upperMessage == null) {
            return false;
        }
        if ("NOT_MATCHES_CUSTOMER_KEY".equals(code)) {
            return false;
        }
        if (code != null) {
            if (code.contains("INVALID_BILLING")
                    || code.contains("NOT_FOUND_BILLING")
                    || code.contains("EXPIRED_BILLING")
                    || code.contains("DELETED_BILLING")
                    || code.contains("BILLING_KEY_NOT_FOUND")
                    || code.contains("NOT_REGISTERED_BILLING")
                    || code.contains("REMOVED_BILLING")) {
                return true;
            }
            if ("NOT_FOUND".equals(code) && containsAny(upperMessage, "BILLING", "AUTO PAY", "AUTOPAY", "자동결제", "빌링")) {
                return true;
            }
        }
        return containsAny(normalizedMessage,
                "INVALID BILLING",
                "INVALID_BILLING",
                "BILLING KEY NOT FOUND",
                "BILLING_KEY_NOT_FOUND",
                "NOT FOUND BILLING",
                "NOT_FOUND_BILLING",
                "DELETED BILLING",
                "DELETED_BILLING",
                "EXPIRED BILLING",
                "EXPIRED_BILLING",
                "BILLINGKEY",
                "BILLING_KEY")
                || containsAny(rawMessage, "빌링키", "삭제된 자동결제", "만료된 자동결제");
    }

    static String normalizeStatus(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private static boolean containsAny(String source, String... needles) {
        if (source == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    record ConfirmResult(
            boolean success,
            String transactionId,
            String failCode,
            String failMessage,
            String rawPayload,
            String methodText,
            String easyPayProvider
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }
    }

    record LookupResult(
            boolean success,
            String failCode,
            String failMessage,
            String rawPayload,
            String paymentKey,
            String orderId,
            String status,
            String methodText,
            String easyPayProvider,
            BigDecimal totalAmount,
            BigDecimal suppliedAmount,
            BigDecimal vat,
            LocalDateTime approvedAt,
            LocalDateTime lastTransactionAt,
            List<CancelHistory> cancels
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }

        public String normalizedStatus() {
            return TossPaymentClient.normalizeStatus(status);
        }
    }

    record CancelHistory(
            BigDecimal cancelAmount,
            String cancelReason,
            LocalDateTime canceledAt,
            String transactionKey,
            String status
    ) {
    }

    record CancelResult(
            boolean success,
            String failCode,
            String failMessage,
            String rawPayload,
            String paymentKey,
            String transactionId,
            BigDecimal cancelAmount,
            LocalDateTime canceledAt,
            String status
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }

        public String normalizedStatus() {
            return TossPaymentClient.normalizeStatus(status);
        }

        public boolean isPartialCancel(BigDecimal originalAmount) {
            return TossPaymentClient.isPartialCanceledStatus(status)
                    || (cancelAmount != null && originalAmount != null && cancelAmount.compareTo(originalAmount) != 0);
        }
    }

    record BillingIssueResult(
            boolean success,
            String failCode,
            String failMessage,
            String rawPayload,
            String customerKey,
            String billingKey,
            String cardCompany,
            String cardNumberMasked,
            String cardType,
            String ownerType
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }
    }

    record BillingChargeResult(
            boolean success,
            String failCode,
            String failMessage,
            String rawPayload,
            String paymentKey,
            String transactionId,
            String orderId,
            String status,
            BigDecimal totalAmount,
            LocalDateTime approvedAt
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }

        public boolean hasInvalidBillingKey() {
            return TossPaymentClient.isInvalidBillingKeyFailure(failCode, failMessage);
        }

        public String normalizedStatus() {
            return TossPaymentClient.normalizeStatus(status);
        }
    }

    record BillingDeleteResult(
            boolean success,
            String failCode,
            String failMessage,
            String rawPayload
    ) {
        public String failureSummary() {
            return TossPaymentClient.summarizeFailure(failCode, failMessage);
        }

        public boolean hasInvalidBillingKey() {
            return TossPaymentClient.isInvalidBillingKeyFailure(failCode, failMessage);
        }
    }
}

