package com.example.project.domain.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "FEE_INVOICE_ITEMS",
        indexes = {
                @Index(name = "IDX_FEE_INVOICE_ITEMS_INVOICE", columnList = "INVOICE_ID"),
                @Index(name = "IDX_FEE_INVOICE_ITEMS_ORDER", columnList = "ORDER_ID")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Long itemId;

    @Column(name = "INVOICE_ID", nullable = false)
    private Long invoiceId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "FEE_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public static FeeInvoiceItem of(Long invoiceId, Long orderId, BigDecimal feeAmount) {
        return FeeInvoiceItem.builder()
                .invoiceId(invoiceId)
                .orderId(orderId)
                .feeAmount(feeAmount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}