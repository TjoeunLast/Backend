package com.example.project.domain.payment.domain.paymentEnum;

public final class PaymentEnums {

    private PaymentEnums() {
    }

    public enum FeeInvoiceStatus {
        ISSUED,
        PAID,
        OVERDUE
    }

    public enum GatewayTxStatus {
        PREPARED,
        CONFIRMED,
        FAILED,
        CANCELED
    }

    public enum PayChannel {
        CARD,
        APP_CARD,
        TRANSFER
    }

    public enum PaymentDisputeReason {
        PRICE_MISMATCH,
        RECEIVED_AMOUNT_MISMATCH,
        PROOF_MISSING,
        FRAUD_SUSPECTED,
        OTHER
    }

    public enum PaymentDisputeStatus {
        PENDING,
        ADMIN_HOLD,
        ADMIN_FORCE_CONFIRMED,
        ADMIN_REJECTED
    }

    public enum PaymentMethod {
        CARD,
        TRANSFER,
        CASH
    }

    public enum PaymentProvider {
        TOSS
    }

    public enum PaymentTiming {
        PREPAID,
        POSTPAID
    }

    public enum PayoutStatus {
        READY,
        REQUESTED,
        COMPLETED,
        FAILED,
        RETRYING
    }

    public enum TransportPaymentStatus {
        READY,
        PAID,
        CONFIRMED,
        DISPUTED,
        ADMIN_HOLD,
        ADMIN_FORCE_CONFIRMED,
        ADMIN_REJECTED,
        CANCELLED
    }
}
