package com.example.project.domain.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "PAYMENT_DISPUTES",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PAYMENT_DISPUTES_ORDER", columnNames = {"ORDER_ID"})
        },
        indexes = {
                @Index(name = "IDX_PAYMENT_DISPUTES_ORDER", columnList = "ORDER_ID"),
                @Index(name = "IDX_PAYMENT_DISPUTES_PAYMENT", columnList = "PAYMENT_ID"),
                @Index(name = "IDX_PAYMENT_DISPUTES_REQUESTER", columnList = "REQUESTER_USER_ID")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentDispute {

    // 이의제기 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DISPUTE_ID")
    private Long disputeId;

    // 대상 주문 ID(주문당 1건)
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    // 대상 결제 ID
    @Column(name = "PAYMENT_ID", nullable = false)
    private Long paymentId;

    // 실제 이의 요청자 ID(차주)
    @Column(name = "REQUESTER_USER_ID", nullable = false)
    private Long requesterUserId;

    // 생성 행위자 ID(차주 본인 또는 관리자 대리)
    @Column(name = "CREATED_BY_USER_ID", nullable = false)
    private Long createdByUserId;

    // 이의 사유 코드
    @Enumerated(EnumType.STRING)
    @Column(name = "REASON_CODE", nullable = false, length = 40)
    private PaymentDisputeReason reasonCode;

    // 상세 설명
    @Column(name = "DESCRIPTION", nullable = false, length = 2000)
    private String description;

    // 첨부 파일 URL
    @Column(name = "ATTACHMENT_URL", length = 500)
    private String attachmentUrl;

    // 이의 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 40)
    private PaymentDisputeStatus status;

    // 관리자 메모
    @Column(name = "ADMIN_MEMO", length = 1000)
    private String adminMemo;

    // 이의 등록 시각
    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    // 관리자 처리 시각
    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;

    // 신규 이의제기 생성(PENDING)
    public static PaymentDispute create(
            Long orderId,
            Long paymentId,
            Long requesterUserId,
            Long createdByUserId,
            PaymentDisputeReason reasonCode,
            String description,
            String attachmentUrl
    ) {
        return PaymentDispute.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .requesterUserId(requesterUserId)
                .createdByUserId(createdByUserId)
                .reasonCode(reasonCode)
                .description(description)
                .attachmentUrl(attachmentUrl)
                .status(PaymentDisputeStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // 상태 및 관리자 메모 갱신
    public void updateStatus(PaymentDisputeStatus status, String adminMemo) {
        this.status = status;
        this.adminMemo = adminMemo;
        this.processedAt = LocalDateTime.now();
    }
}
