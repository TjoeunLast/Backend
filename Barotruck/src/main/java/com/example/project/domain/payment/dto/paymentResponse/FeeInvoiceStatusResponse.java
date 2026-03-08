package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeInvoiceStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FeeInvoiceStatusResponse(
        Long invoiceId,
        Long shipperUserId,
        String period,
        FeeInvoiceStatus status,
        BigDecimal totalFee,
        LocalDateTime issuedAt,
        LocalDateTime dueAt,
        LocalDateTime paidAt
) {
    public static FeeInvoiceStatusResponse from(FeeInvoice invoice) {
        return FeeInvoiceStatusResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .shipperUserId(invoice.getShipperUserId())
                .period(invoice.getPeriod())
                .status(invoice.getStatus())
                .totalFee(invoice.getTotalFee())
                .issuedAt(invoice.getIssuedAt())
                .dueAt(invoice.getDueAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }
}
