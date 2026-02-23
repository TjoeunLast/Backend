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

    // 인보이스 항목 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Long itemId;

    // 상위 인보이스 ID
    @Column(name = "INVOICE_ID", nullable = false)
    private Long invoiceId;

    // 수수료가 발생한 주문 ID
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    // 주문별 수수료 금액
    @Column(name = "FEE_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount;

    // 항목 생성 시각
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // 주문 단위 인보이스 항목 생성
    public static FeeInvoiceItem of(Long invoiceId, Long orderId, BigDecimal feeAmount) {
        return FeeInvoiceItem.builder()
                .invoiceId(invoiceId)
                .orderId(orderId)
                .feeAmount(feeAmount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
