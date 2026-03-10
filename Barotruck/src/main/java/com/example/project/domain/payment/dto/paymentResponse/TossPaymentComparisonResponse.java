package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.TransportPayment;
import lombok.Builder;

@Builder
public record TossPaymentComparisonResponse(
        GatewayTransactionStatusResponse gatewayTransaction,
        TransportPaymentResponse transportPayment,
        TossPaymentLookupResponse gatewayLookup,
        boolean mismatch,
        String mismatchReason
) {
    public static TossPaymentComparisonResponse of(
            GatewayTransactionStatusResponse gatewayTransaction,
            TransportPayment transportPayment,
            TossPaymentLookupResponse gatewayLookup,
            boolean mismatch,
            String mismatchReason
    ) {
        return TossPaymentComparisonResponse.builder()
                .gatewayTransaction(gatewayTransaction)
                .transportPayment(transportPayment == null ? null : TransportPaymentResponse.from(transportPayment))
                .gatewayLookup(gatewayLookup)
                .mismatch(mismatch)
                .mismatchReason(mismatchReason)
                .build();
    }
}
