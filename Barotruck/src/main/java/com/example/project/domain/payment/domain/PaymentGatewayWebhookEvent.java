package com.example.project.domain.payment.domain;

import com.example.project.domain.payment.domain.paymentEnum.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "PAYMENT_GATEWAY_WEBHOOK_EVENTS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PGWEBHOOK_PROVIDER_EVENT", columnNames = {"PROVIDER", "EXTERNAL_EVENT_ID"})
        },
        indexes = {
                @Index(name = "IDX_PGWEBHOOK_PROVIDER", columnList = "PROVIDER"),
                @Index(name = "IDX_PGWEBHOOK_EVENT_TYPE", columnList = "EVENT_TYPE"),
                @Index(name = "IDX_PGWEBHOOK_RECEIVED_AT", columnList = "RECEIVED_AT")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentGatewayWebhookEvent {

    // 웹훅 이벤트 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WEBHOOK_ID")
    private Long webhookId;

    // PG 제공사
    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false, length = 30)
    private PaymentProvider provider;

    // 외부 이벤트 고유 ID
    @Column(name = "EXTERNAL_EVENT_ID", nullable = false, length = 200)
    private String externalEventId;

    // 이벤트 타입
    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    private String eventType;

    // 웹훅 원본 payload
    @Lob
    @Column(name = "PAYLOAD", nullable = false)
    private String payload;

    // 수신 시각
    @Column(name = "RECEIVED_AT", nullable = false)
    private LocalDateTime receivedAt;

    // 처리 완료 시각
    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;

    // 처리 결과 코드/메시지
    @Column(name = "PROCESS_RESULT", length = 200)
    private String processResult;

    @PrePersist
    void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = LocalDateTime.now();
        }
    }

    // 처리 완료 마킹
    public void markProcessed(String result) {
        this.processResult = result;
        this.processedAt = LocalDateTime.now();
    }
}
