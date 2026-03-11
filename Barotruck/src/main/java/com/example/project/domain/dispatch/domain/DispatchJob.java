package com.example.project.domain.dispatch.domain;

import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobStatus;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobType;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchMode;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DISPATCH_JOBS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DISPATCH_JOB_ID")
    private Long dispatchJobId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_TYPE", nullable = false, length = 20)
    private DispatchJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private DispatchJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPATCH_MODE", nullable = false, length = 20)
    private DispatchMode dispatchMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPATCH_PRIORITY", nullable = false, length = 20)
    private DispatchPriority dispatchPriority;

    @Builder.Default
    @Column(name = "WAVE", nullable = false)
    private Integer wave = 0;

    @Column(name = "MATCHED_DRIVER_USER_ID")
    private Long matchedDriverUserId;

    @Column(name = "CURRENT_CANDIDATE_COUNT")
    private Integer currentCandidateCount;

    @Column(name = "FAILURE_REASON_CODE", length = 50)
    private String failureReasonCode;

    @Column(name = "FAILURE_REASON_MESSAGE", length = 500)
    private String failureReasonMessage;

    @Lob
    @Column(name = "DEBUG_SNAPSHOT_JSON")
    private String debugSnapshotJson;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "LAST_WAVE_STARTED_AT")
    private LocalDateTime lastWaveStartedAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @Builder.Default
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active = true;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public void markSearching() {
        ensureNotTerminal();
        this.status = DispatchJobStatus.SEARCHING;
        this.failureReasonCode = null;
        this.failureReasonMessage = null;
    }

    public void markOffering() {
        ensureNotTerminal();
        this.status = DispatchJobStatus.OFFERING;
    }

    public void markWaitingResponse(int nextWave, int candidateCount, LocalDateTime expireAt, String debugSnapshotJson) {
        ensureNotTerminal();
        this.wave = nextWave;
        this.status = DispatchJobStatus.WAITING_RESPONSE;
        this.currentCandidateCount = candidateCount;
        this.expiresAt = expireAt;
        this.lastWaveStartedAt = LocalDateTime.now();
        this.debugSnapshotJson = debugSnapshotJson;
    }

    public void markMatched(Long driverUserId) {
        ensureNotTerminal();
        this.status = DispatchJobStatus.MATCHED;
        this.matchedDriverUserId = driverUserId;
        this.active = false;
        this.closedAt = LocalDateTime.now();
        this.expiresAt = null;
    }

    public void markFailed(String reasonCode, String reasonMessage) {
        ensureNotTerminal();
        this.status = DispatchJobStatus.FAILED;
        this.failureReasonCode = reasonCode;
        this.failureReasonMessage = reasonMessage;
        this.active = false;
        this.closedAt = LocalDateTime.now();
        this.expiresAt = null;
    }

    public void cancel(String reasonCode, String reasonMessage) {
        if (this.status == DispatchJobStatus.CANCELLED) {
            return;
        }
        this.status = DispatchJobStatus.CANCELLED;
        this.failureReasonCode = reasonCode;
        this.failureReasonMessage = reasonMessage;
        this.active = false;
        this.closedAt = LocalDateTime.now();
        this.expiresAt = null;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active) && status != null && !status.isTerminal();
    }

    private void ensureNotTerminal() {
        if (status != null && status.isTerminal()) {
            throw new IllegalStateException("dispatch job is already terminal: " + status);
        }
    }
}
