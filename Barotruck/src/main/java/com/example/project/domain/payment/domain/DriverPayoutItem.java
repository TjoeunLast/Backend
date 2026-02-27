package com.example.project.domain.payment.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "DRIVER_PAYOUT_ITEMS",
        indexes = {
                @Index(name = "IDX_DRIVER_PAYOUT_ITEM_BATCH_ID", columnList = "BATCH_ID"),
                @Index(name = "IDX_DRIVER_PAYOUT_ITEM_ORDER_ID", columnList = "ORDER_ID"),
                @Index(name = "IDX_DRIVER_PAYOUT_ITEM_DRIVER_ID", columnList = "DRIVER_USER_ID"),
                @Index(name = "IDX_DRIVER_PAYOUT_ITEM_STATUS", columnList = "STATUS")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_DRIVER_PAYOUT_ITEM_ORDER_ID", columnNames = {"ORDER_ID"})
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverPayoutItem {

    // 지급 아이템 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Long itemId;

    // 지급 배치
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BATCH_ID", nullable = false)
    @JsonIgnore
    private DriverPayoutBatch batch;

    // 주문 ID
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    // 차주 사용자 ID
    @Column(name = "DRIVER_USER_ID", nullable = false)
    private Long driverUserId;

    // 차주 지급 금액
    @Column(name = "NET_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount;

    // 지급 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PayoutStatus status;

    // 지급 요청 시각
    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    // 지급 완료 시각
    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    // 실패 사유
    @Column(name = "FAILURE_REASON", length = 2000)
    private String failureReason;

    // 재시도 횟수
    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount;

    // 외부 지급 참조값
    @Column(name = "PAYOUT_REF", length = 200)
    private String payoutRef;

    @PrePersist
    void onCreate() {
        if (this.status == null) {
            this.status = PayoutStatus.READY;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    // 지급 준비 상태 생성
    public static DriverPayoutItem ready(DriverPayoutBatch batch, Long orderId, Long driverUserId, BigDecimal netAmount) {
        return DriverPayoutItem.builder()
                .batch(batch)
                .orderId(orderId)
                .driverUserId(driverUserId)
                .netAmount(netAmount)
                .status(PayoutStatus.READY)
                .retryCount(0)
                .build();
    }

    // 지급 요청 상태로 변경
    public void markRequested() {
        this.status = PayoutStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    // 지급 완료 상태로 변경
    public void markCompleted(String payoutRef) {
        this.status = PayoutStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = null;
        this.payoutRef = payoutRef;
    }

    // 지급 실패 상태로 변경
    public void markFailed(String reason) {
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason;
        this.retryCount = this.retryCount + 1;
    }

    // 재시도 상태로 변경
    public void markRetrying() {
        this.status = PayoutStatus.RETRYING;
        this.requestedAt = LocalDateTime.now();
    }
}
