package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.FeeInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "FEE_INVOICES",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_FEE_INVOICES_SHIPPER_PERIOD", columnNames = {"SHIPPER_USER_ID","PERIOD"})
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeInvoice {

    // 인보이스 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVOICE_ID")
    private Long invoiceId;

    // 화주 사용자 ID
    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    // 정산 월(YYYY-MM)
    @Column(name = "PERIOD", nullable = false, length = 7)
    private String period;

    // 해당 월 총 수수료 금액
    @Column(name = "TOTAL_FEE", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalFee;

    // 인보이스 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private FeeInvoiceStatus status;

    // 발행 시각
    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    // 납부 기한
    @Column(name = "DUE_AT")
    private LocalDateTime dueAt;

    // 납부 완료 시각
    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    // 인보이스 발행
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

    // 총 수수료 갱신
    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    // 납부 완료 처리
    public void markPaid() {
        this.status = FeeInvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    // 미납/연체 처리
    public void markOverdue() {
        if (this.status == FeeInvoiceStatus.PAID) {
            return;
        }
        this.status = FeeInvoiceStatus.OVERDUE;
    }
}
