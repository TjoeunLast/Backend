package com.example.project.domain.dispatch.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class DispatchDtos {

    private DispatchDtos() {
    }

    public record DispatchRunRequest(Boolean forceRebuild, String reason) {
    }

    public record DispatchRetryRequest(String jobType, String reasonCode) {
    }

    public record DriverAvailabilityUpdateRequest(String availabilityStatus) {
    }

    public record DriverLocationUpdateRequest(Double lat, Double lng, String recordedAt) {
    }

    public record DispatchOfferRejectRequest(String reasonCode) {
    }

    public record DispatchOfferDecisionResponse(
            Long offerId,
            Long dispatchJobId,
            Long orderId,
            boolean accepted,
            String jobStatus,
            String orderStatus
    ) {
    }

    public record DriverDispatchOfferResponse(
            Long offerId,
            Long dispatchJobId,
            Long orderId,
            String orderStatus,
            String status,
            Integer wave,
            Integer rank,
            BigDecimal score,
            BigDecimal distanceKm,
            Integer etaMinutes,
            LocalDateTime expireAt,
            String scoreBreakdownJson
    ) {
    }

    @Builder
    public record DispatchStatusResponse(
            Long orderId,
            String orderStatus,
            String dispatchPublicStatus,
            JobSnapshot job,
            Summary summary,
            MatchedDriver matchedDriver,
            List<OfferSnapshot> offers
    ) {
    }

    @Builder
    public record JobSnapshot(
            Long dispatchJobId,
            String jobType,
            String status,
            Integer wave,
            String dispatchMode,
            String dispatchPriority,
            Integer candidateCount,
            Long matchedDriverUserId,
            String failureReasonCode,
            String failureReasonMessage,
            LocalDateTime startedAt,
            LocalDateTime lastWaveStartedAt,
            LocalDateTime expiresAt,
            LocalDateTime closedAt
    ) {
    }

    @Builder
    public record Summary(
            int offersSent,
            int offersOpen,
            int offersRejected,
            int offersExpired,
            int offersCancelled
    ) {
    }

    @Builder
    public record MatchedDriver(
            Long driverUserId,
            Long driverId,
            String nickname,
            String carNum,
            String carType,
            BigDecimal tonnage
    ) {
    }

    @Builder
    public record OfferSnapshot(
            Long offerId,
            Long driverUserId,
            Integer wave,
            Integer rank,
            String status,
            BigDecimal score,
            BigDecimal distanceKm,
            Integer etaMinutes,
            LocalDateTime sentAt,
            LocalDateTime expireAt,
            LocalDateTime respondedAt,
            String rejectReasonCode,
            String closedReason,
            String scoreBreakdownJson
    ) {
    }

    public record DispatchJobListItemResponse(
            Long dispatchJobId,
            Long orderId,
            String jobType,
            String status,
            Integer wave,
            String failureReasonCode,
            LocalDateTime startedAt,
            LocalDateTime expiresAt,
            LocalDateTime closedAt
    ) {
    }
}
