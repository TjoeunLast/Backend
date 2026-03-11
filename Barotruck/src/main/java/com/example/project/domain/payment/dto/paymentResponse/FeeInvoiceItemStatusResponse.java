package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FeeInvoiceItemStatusResponse(
        Long itemId,
        Long orderId,
        PaymentAmountSnapshotResponse amountSnapshot,
        LocalDateTime createdAt
) {
    public static FeeInvoiceItemStatusResponse from(FeeInvoiceItem item, TransportPayment payment) {
        return FeeInvoiceItemStatusResponse.builder()
                .itemId(item.getItemId())
                .orderId(item.getOrderId())
                .amountSnapshot(PaymentAmountSnapshotResponse.merge(payment, item, null))
                .createdAt(item.getCreatedAt())
                .build();
    }
}
