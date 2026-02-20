package com.example.project.domain.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "FEE_INVOICES",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_FEE_INVOICES_SHIPPER_PERIOD", columnNames = {"SHIPPER_USER_ID","PERIOD"})
        },
        indexes = {
                @Index(name = "IDX_FEE_INVOICES_SHIPPER_PERIOD", columnList = "SHIPPER_USER_ID,PERIOD")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVOICE_ID")
    private Long invoiceId;

    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    // YYYY-MM
    @Column(name = "PERIOD", nullable = false, length = 7)
    private String period;

    @Column(name = "TOTAL_FEE", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private FeeInvoiceStatus status;

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "DUE_AT")
    private LocalDateTime dueAt;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    public static FeeInvoice issue(Long shipperUserId, String period, BigDecimal totalFee, LocalDateTime dueAt) {
        return FeeInvoice.builder()
                .shipperUserId(shipperUserId)
                .period(period)
                .totalFee(totalFee)
                .status(FeeInvoiceStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .dueAt(dueAt)
                .build();
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public void markPaid() {
        this.status = FeeInvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
}