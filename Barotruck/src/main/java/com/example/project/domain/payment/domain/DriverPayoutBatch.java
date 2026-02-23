package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "DRIVER_PAYOUT_BATCHES",
        indexes = {
                @Index(name = "IDX_DRIVER_PAYOUT_BATCH_DATE", columnList = "BATCH_DATE"),
                @Index(name = "IDX_DRIVER_PAYOUT_BATCH_STATUS", columnList = "STATUS")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverPayoutBatch {

    // 지급 배치 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BATCH_ID")
    private Long batchId;

    // 배치 기준 일자(정산일)
    @Column(name = "BATCH_DATE", nullable = false)
    private LocalDate batchDate;

    // 배치 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PayoutStatus status;

    // 배치 요청 시각
    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    // 배치 완료 시각
    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    // 배치 실패 사유
    @Column(name = "FAILURE_REASON", length = 2000)
    private String failureReason;

    @PrePersist
    void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PayoutStatus.READY;
        }
    }

    // 지급 배치 시작
    public static DriverPayoutBatch start(LocalDate date) {
        return DriverPayoutBatch.builder()
                .batchDate(date)
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // 배치 성공 완료 처리
    public void markCompleted() {
        this.status = PayoutStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    // 배치 실패 처리
    public void markFailed(String reason) {
        this.status = PayoutStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = reason;
    }
}
