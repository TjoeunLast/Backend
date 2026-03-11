package com.example.project.domain.dispatch.domain;

import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchOfferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DISPATCH_OFFERS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DISPATCH_OFFER_ID")
    private Long dispatchOfferId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DISPATCH_JOB_ID")
    private DispatchJob job;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "DRIVER_USER_ID", nullable = false)
    private Long driverUserId;

    @Column(name = "WAVE", nullable = false)
    private Integer wave;

    @Column(name = "RANK_NO", nullable = false)
    private Integer rank;

    @Column(name = "SCORE", precision = 12, scale = 2)
    private BigDecimal score;

    @Column(name = "DISTANCE_KM", precision = 12, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "ETA_MINUTES")
    private Integer etaMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private DispatchOfferStatus status;

    @Column(name = "REJECT_REASON_CODE", length = 50)
    private String rejectReasonCode;

    @Lob
    @Column(name = "SCORE_BREAKDOWN_JSON")
    private String scoreBreakdownJson;

    @Lob
    @Column(name = "CANDIDATE_SNAPSHOT_JSON")
    private String candidateSnapshotJson;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    @Column(name = "OPENED_AT")
    private LocalDateTime openedAt;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime respondedAt;

    @Column(name = "EXPIRE_AT", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "CLOSED_REASON", length = 100)
    private String closedReason;

    public void markPushSent() {
        if (status == DispatchOfferStatus.PENDING) {
            this.status = DispatchOfferStatus.PUSH_SENT;
            this.sentAt = LocalDateTime.now();
        }
    }

    public void markOpened() {
        if (status == DispatchOfferStatus.PUSH_SENT) {
            this.status = DispatchOfferStatus.OPENED;
            this.openedAt = LocalDateTime.now();
        }
    }

    public void markAccepted() {
        ensureOpen();
        this.status = DispatchOfferStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
        this.closedReason = "accepted";
    }

    public void markRejected(String reasonCode) {
        ensureOpen();
        this.status = DispatchOfferStatus.REJECTED;
        this.rejectReasonCode = reasonCode;
        this.respondedAt = LocalDateTime.now();
        this.closedReason = "rejected";
    }

    public void markExpired() {
        if (!status.isTerminal()) {
            this.status = DispatchOfferStatus.EXPIRED;
            this.respondedAt = LocalDateTime.now();
            this.closedReason = "expired";
        }
    }

    public void cancel(String reason) {
        if (!status.isTerminal()) {
            this.status = DispatchOfferStatus.CANCELLED;
            this.respondedAt = LocalDateTime.now();
            this.closedReason = reason;
        }
    }

    public boolean isOpen() {
        return status == DispatchOfferStatus.PENDING
                || status == DispatchOfferStatus.PUSH_SENT
                || status == DispatchOfferStatus.OPENED;
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("dispatch offer is not open: " + status);
        }
    }
}
