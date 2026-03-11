package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeInvoiceStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Builder
public record FeeInvoiceStatusResponse(
        Long invoiceId,
        Long shipperUserId,
        String period,
        FeeInvoiceStatus status,
        BigDecimal totalFee,
        Long itemCount,
        BigDecimal totalShipperChargeAmount,
        BigDecimal totalDriverPayoutAmount,
        BigDecimal totalDriverFeeAmount,
        BigDecimal totalTossFeeAmount,
        BigDecimal totalPlatformGrossRevenue,
        BigDecimal totalPlatformNetRevenue,
        List<FeeInvoiceItemStatusResponse> items,
        LocalDateTime issuedAt,
        LocalDateTime dueAt,
        LocalDateTime paidAt
) {
    public static FeeInvoiceStatusResponse from(FeeInvoice invoice) {
        return from(invoice, Collections.emptyList(), Collections.emptyMap());
    }

    public static FeeInvoiceStatusResponse from(
            FeeInvoice invoice,
            List<FeeInvoiceItem> invoiceItems,
            Map<Long, TransportPayment> paymentsByOrderId
    ) {
        List<FeeInvoiceItemStatusResponse> items = invoiceItems == null
                ? List.of()
                : invoiceItems.stream()
                .map(item -> FeeInvoiceItemStatusResponse.from(
                        item,
                        paymentsByOrderId == null ? null : paymentsByOrderId.get(item.getOrderId())
                ))
                .toList();

        return FeeInvoiceStatusResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .shipperUserId(invoice.getShipperUserId())
                .period(invoice.getPeriod())
                .status(invoice.getStatus())
                .totalFee(invoice.getTotalFee())
                .itemCount((long) items.size())
                .totalShipperChargeAmount(sum(items, snapshot -> snapshot.shipperChargeAmount()))
                .totalDriverPayoutAmount(sum(items, snapshot -> snapshot.driverPayoutAmount()))
                .totalDriverFeeAmount(sum(items, snapshot -> snapshot.driverFeeAmount()))
                .totalTossFeeAmount(sum(items, snapshot -> snapshot.tossFeeAmount()))
                .totalPlatformGrossRevenue(sum(items, snapshot -> snapshot.platformGrossRevenue()))
                .totalPlatformNetRevenue(sum(items, snapshot -> snapshot.platformNetRevenue()))
                .items(items)
                .issuedAt(invoice.getIssuedAt())
                .dueAt(invoice.getDueAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }

    private static BigDecimal sum(
            List<FeeInvoiceItemStatusResponse> items,
            java.util.function.Function<PaymentAmountSnapshotResponse, BigDecimal> extractor
    ) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(FeeInvoiceItemStatusResponse::amountSnapshot)
                .map(extractor)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
