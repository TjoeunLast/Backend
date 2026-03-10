package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "PAYMENT_GATEWAY_TRANSACTIONS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PGTX_PROVIDER_PG_ORDER_ID", columnNames = {"PROVIDER", "PG_ORDER_ID"})
        },
        indexes = {
                @Index(name = "IDX_PGTX_ORDER_ID", columnList = "ORDER_ID"),
                @Index(name = "IDX_PGTX_SHIPPER_USER_ID", columnList = "SHIPPER_USER_ID"),
                @Index(name = "IDX_PGTX_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_PGTX_PROVIDER_PAYMENT_KEY", columnList = "PROVIDER, PAYMENT_KEY")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentGatewayTransaction {

    // PG 거래 레코드 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TX_ID")
    private Long txId;

    // 대상 주문 ID
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    // 결제 요청 화주 ID
    @Column(name = "SHIPPER_USER_ID", nullable = false)
    private Long shipperUserId;

    // PG 제공사
    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false, length = 30)
    private PaymentProvider provider;

    // 도메인 결제 수단
    @Enumerated(EnumType.STRING)
    @Column(name = "METHOD", nullable = false, length = 20)
    private PaymentMethod method;

    // 결제 채널(앱카드/카드/이체)
    @Enumerated(EnumType.STRING)
    @Column(name = "PAY_CHANNEL", nullable = false, length = 20)
    private PayChannel payChannel;

    // PG 주문번호
    @Column(name = "PG_ORDER_ID", nullable = false, length = 120)
    private String pgOrderId;

    // PG paymentKey
    @Column(name = "PAYMENT_KEY", length = 200)
    private String paymentKey;

    // 승인 요청 금액
    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // 게이트웨이 거래 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private GatewayTxStatus status;

    // 멱등 처리 키
    @Column(name = "IDEMPOTENCY_KEY", nullable = false, length = 120)
    private String idempotencyKey;

    // 외부 거래 참조값
    @Column(name = "TRANSACTION_ID", length = 200)
    private String transactionId;

    // 결제 성공 리다이렉트 URL
    @Column(name = "SUCCESS_URL", length = 500)
    private String successUrl;

    // 결제 실패 리다이렉트 URL
    @Column(name = "FAIL_URL", length = 500)
    private String failUrl;

    // 결제 만료 시각
    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    // 승인 완료 시각
    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    // PG가 마지막으로 알려준 상태 텍스트
    @Column(name = "GATEWAY_STATUS", length = 100)
    private String gatewayStatus;

    // 취소 사유
    @Column(name = "CANCEL_REASON", length = 1000)
    private String cancelReason;

    // 취소 금액
    @Column(name = "CANCELED_AMOUNT", precision = 18, scale = 2)
    private BigDecimal canceledAmount;

    // 취소 완료 시각
    @Column(name = "CANCELED_AT")
    private LocalDateTime canceledAt;

    // 취소 거래 식별자
    @Column(name = "CANCEL_TRANSACTION_ID", length = 200)
    private String cancelTransactionId;

    // 실패 코드
    @Column(name = "FAIL_CODE", length = 100)
    private String failCode;

    // 실패 메시지
    @Column(name = "FAIL_MESSAGE", length = 2000)
    private String failMessage;

    // 재시도 횟수
    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount;

    // 마지막 재시도 시각
    @Column(name = "LAST_RETRY_AT")
    private LocalDateTime lastRetryAt;

    // 다음 재시도 가능 시각
    @Column(name = "NEXT_RETRY_AT")
    private LocalDateTime nextRetryAt;

    // PG 원본 응답 payload
    @Lob
    @Column(name = "RAW_PAYLOAD")
    private String rawPayload;

    // 생성 시각
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // 최종 수정 시각
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 결제 준비 거래 생성
    public static PaymentGatewayTransaction prepare(
            Long orderId,
            Long shipperUserId,
            PaymentProvider provider,
            PaymentMethod method,
            PayChannel payChannel,
            String pgOrderId,
            BigDecimal amount,
            String idempotencyKey,
            String successUrl,
            String failUrl,
            LocalDateTime expiresAt
    ) {
        return PaymentGatewayTransaction.builder()
                .orderId(orderId)
                .shipperUserId(shipperUserId)
                .provider(provider)
                .method(method)
                .payChannel(payChannel)
                .pgOrderId(pgOrderId)
                .amount(amount)
                .status(GatewayTxStatus.PREPARED)
                .idempotencyKey(idempotencyKey)
                .successUrl(successUrl)
                .failUrl(failUrl)
                .expiresAt(expiresAt)
                .retryCount(0)
                .build();
    }

    // 승인 성공 반영
    public void markConfirmed(String paymentKey, String transactionId, String rawPayload) {
        this.paymentKey = paymentKey;
        this.transactionId = transactionId;
        this.rawPayload = rawPayload;
        this.failCode = null;
        this.failMessage = null;
        this.status = GatewayTxStatus.CONFIRMED;
        this.gatewayStatus = GatewayTxStatus.CONFIRMED.name();
        this.approvedAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    // 승인 실패 반영
    public void markFailed(String failCode, String failMessage, String rawPayload) {
        markFailed(failCode, failMessage, rawPayload, true);
    }

    public void markFailed(String failCode, String failMessage, String rawPayload, boolean retryable) {
        this.failCode = failCode;
        this.failMessage = failMessage;
        this.rawPayload = rawPayload;
        this.status = GatewayTxStatus.FAILED;
        this.gatewayStatus = GatewayTxStatus.FAILED.name();
        if (retryable) {
            scheduleNextRetry();
        } else {
            this.lastRetryAt = LocalDateTime.now();
            this.nextRetryAt = null;
        }
    }

    // 취소 반영
    public void markCanceled(String rawPayload) {
        markCanceled(rawPayload, null, null, null, null);
    }

    public void markCanceled(
            String rawPayload,
            String cancelReason,
            BigDecimal canceledAmount,
            LocalDateTime canceledAt,
            String cancelTransactionId
    ) {
        this.rawPayload = rawPayload;
        this.status = GatewayTxStatus.CANCELED;
        this.gatewayStatus = GatewayTxStatus.CANCELED.name();
        this.cancelReason = cancelReason;
        this.canceledAmount = canceledAmount;
        this.canceledAt = canceledAt == null ? LocalDateTime.now() : canceledAt;
        this.cancelTransactionId = cancelTransactionId;
        this.nextRetryAt = null;
    }

    public void markExpired() {
        markFailed("TX_EXPIRED", "prepared transaction expired", this.rawPayload, false);
    }

    public void bindPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            return;
        }
        this.paymentKey = paymentKey;
    }

    public void applyMethodAndChannel(PaymentMethod method, PayChannel payChannel) {
        if (method != null) {
            this.method = method;
        }
        if (payChannel != null) {
            this.payChannel = payChannel;
        }
    }

    public void applyGatewayStatus(String gatewayStatus) {
        if (gatewayStatus == null || gatewayStatus.isBlank()) {
            return;
        }
        this.gatewayStatus = gatewayStatus;
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expiresAt != null && now != null && now.isAfter(this.expiresAt);
    }

    public boolean canRetry(LocalDateTime now, int maxRetries) {
        if (this.status != GatewayTxStatus.FAILED) {
            return false;
        }
        if (now == null || maxRetries <= 0) {
            return false;
        }
        int attempts = this.retryCount == null ? 0 : this.retryCount;
        if (attempts >= maxRetries) {
            return false;
        }
        if (this.paymentKey == null || this.paymentKey.isBlank()) {
            return false;
        }
        return this.nextRetryAt != null && !this.nextRetryAt.isAfter(now);
    }

    public void stopRetry(String failCode, String failMessage) {
        this.status = GatewayTxStatus.FAILED;
        this.failCode = failCode;
        this.failMessage = failMessage;
        this.lastRetryAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    private void scheduleNextRetry() {
        int attempts = this.retryCount == null ? 0 : this.retryCount;
        this.retryCount = attempts + 1;
        this.lastRetryAt = LocalDateTime.now();
        long backoffMinutes = Math.min(60L, 1L << Math.min(this.retryCount - 1, 6));
        this.nextRetryAt = this.lastRetryAt.plusMinutes(backoffMinutes);
    }
}
