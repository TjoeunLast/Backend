package com.example.project.domain.dispatch.service.core;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.DispatchOffer;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobStatus;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobType;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchMode;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchOfferStatus;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchPriority;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchOfferDecisionResponse;
import com.example.project.domain.dispatch.repository.DispatchJobRepository;
import com.example.project.domain.dispatch.repository.DispatchOfferRepository;
import com.example.project.domain.dispatch.service.availability.DispatchAvailabilityService;
import com.example.project.domain.dispatch.service.scoring.DispatchScoringService;
import com.example.project.domain.dispatch.service.scoring.DispatchScoringService.ScoredDriverCandidate;
import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatchOrchestratorService {

    private static final List<DispatchOfferStatus> OPEN_OFFER_STATUSES = List.of(
            DispatchOfferStatus.PENDING,
            DispatchOfferStatus.PUSH_SENT,
            DispatchOfferStatus.OPENED
    );

    private final DispatchJobRepository dispatchJobRepository;
    private final DispatchOfferRepository dispatchOfferRepository;
    private final OrderRepository orderRepository;
    private final DispatchAvailabilityService availabilityService;
    private final DispatchScoringService dispatchScoringService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${dispatch.auto-enabled:true}")
    private boolean autoDispatchEnabled;

    @Transactional
    public DispatchJob startAutoDispatch(Long orderId, boolean forceRebuild, String reason) {
        if (!autoDispatchEnabled) {
            throw new IllegalStateException("auto dispatch is disabled");
        }
        Order order = getDispatchableOrder(orderId);
        ensureAutoDispatchUnlocked(order);
        DispatchJob activeJob = dispatchJobRepository.findFirstByOrderIdAndActiveTrueOrderByStartedAtDesc(orderId).orElse(null);
        if (activeJob != null) {
            if (!forceRebuild) {
                return activeJob;
            }
            closeJob(activeJob, "REBUILD", defaultMessage(reason, "dispatch rebuild requested"));
        }

        DispatchJob job = dispatchJobRepository.save(DispatchJob.builder()
                .orderId(orderId)
                .jobType(DispatchJobType.INITIAL)
                .status(DispatchJobStatus.QUEUED)
                .dispatchMode(DispatchMode.AUTO_OFFER)
                .dispatchPriority(resolvePriority(order))
                .wave(0)
                .active(true)
                .startedAt(LocalDateTime.now())
                .build());
        return launchNextWave(job.getDispatchJobId());
    }

    @Transactional
    public DispatchJob retryDispatch(Long orderId, DispatchJobType jobType, String reasonCode) {
        Order order = getDispatchableOrder(orderId);
        ensureAutoDispatchUnlocked(order);
        dispatchJobRepository.findFirstByOrderIdAndActiveTrueOrderByStartedAtDesc(orderId)
                .ifPresent(job -> closeJob(job, "RETRY_REPLACED", defaultMessage(reasonCode, "retry replaced active job")));
        DispatchJob job = dispatchJobRepository.save(DispatchJob.builder()
                .orderId(order.getOrderId())
                .jobType(jobType == null ? DispatchJobType.REASSIGN : jobType)
                .status(DispatchJobStatus.QUEUED)
                .dispatchMode(DispatchMode.AUTO_OFFER)
                .dispatchPriority(resolvePriority(order))
                .wave(0)
                .active(true)
                .startedAt(LocalDateTime.now())
                .build());
        sendNotificationSafely(order.getUser().getUserId(), "ORDER", "재배차 시작", "기사 재배차를 다시 시작합니다.", order.getOrderId());
        return launchNextWave(job.getDispatchJobId());
    }

    @Transactional
    public void cancelActiveDispatch(Long orderId, String reason) {
        dispatchJobRepository.findFirstByOrderIdAndActiveTrueOrderByStartedAtDesc(orderId)
                .ifPresent(job -> closeJob(job, "CANCELLED", defaultMessage(reason, "dispatch cancelled")));
    }

    @Transactional
    public DispatchOfferDecisionResponse acceptOffer(Long offerId, Long driverUserId) {
        DispatchOffer offer = dispatchOfferRepository.findByDispatchOfferIdAndDriverUserId(offerId, driverUserId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch offer not found"));
        DispatchJob job = dispatchJobRepository.findByIdForUpdate(offer.getJob().getDispatchJobId())
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found"));
        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found"));

        if (job.getStatus() == DispatchJobStatus.MATCHED
                && Objects.equals(job.getMatchedDriverUserId(), driverUserId)
                && offer.getStatus() == DispatchOfferStatus.ACCEPTED) {
            return new DispatchOfferDecisionResponse(offer.getDispatchOfferId(), job.getDispatchJobId(), order.getOrderId(), true,
                    job.getStatus().name(), order.getStatus());
        }
        if (!job.isActive()) {
            throw new IllegalStateException("dispatch job already closed");
        }
        if (!"REQUESTED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("order is no longer dispatchable");
        }
        if (offer.getExpireAt() != null && offer.getExpireAt().isBefore(LocalDateTime.now())) {
            offer.markExpired();
            dispatchOfferRepository.save(offer);
            throw new IllegalStateException("dispatch offer expired");
        }
        if (!offer.isOpen()) {
            throw new IllegalStateException("dispatch offer already closed");
        }

        offer.markAccepted();
        dispatchOfferRepository.save(offer);
        cancelOtherOpenOffers(job, offer.getDispatchOfferId(), "matched_elsewhere");

        job.markMatched(driverUserId);
        dispatchJobRepository.save(job);

        order.assignDriver(driverUserId, "ACCEPTED");
        order.getDriverList().clear();
        orderRepository.save(order);
        availabilityService.markMatched(driverUserId, order.getOrderId(), order.getStatus());

        sendNotificationSafely(order.getUser().getUserId(), "ORDER", "배차 확정", "자동배차로 기사 배정이 확정되었습니다.", order.getOrderId());
        sendNotificationSafely(driverUserId, "ORDER", "배차 수락 완료", "오더가 회원님에게 최종 배정되었습니다.", order.getOrderId());

        return new DispatchOfferDecisionResponse(
                offer.getDispatchOfferId(),
                job.getDispatchJobId(),
                order.getOrderId(),
                true,
                job.getStatus().name(),
                order.getStatus()
        );
    }

    @Transactional
    public DispatchOfferDecisionResponse rejectOffer(Long offerId, Long driverUserId, String reasonCode) {
        DispatchOffer offer = dispatchOfferRepository.findByDispatchOfferIdAndDriverUserId(offerId, driverUserId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch offer not found"));
        DispatchJob job = dispatchJobRepository.findByIdForUpdate(offer.getJob().getDispatchJobId())
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found"));
        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found"));

        if (offer.getStatus() == DispatchOfferStatus.REJECTED) {
            return new DispatchOfferDecisionResponse(offer.getDispatchOfferId(), job.getDispatchJobId(), order.getOrderId(), false,
                    job.getStatus().name(), order.getStatus());
        }
        if (!offer.isOpen()) {
            throw new IllegalStateException("dispatch offer already closed");
        }

        offer.markRejected(defaultMessage(reasonCode, "REJECTED"));
        dispatchOfferRepository.save(offer);

        if (job.isActive() && !hasOpenOffers(job)) {
            if (job.getWave() >= maxWave()) {
                job.markFailed("NO_RESPONSE", "모든 웨이브에서 기사 응답을 확보하지 못했습니다.");
                dispatchJobRepository.save(job);
                sendNotificationSafely(order.getUser().getUserId(), "ORDER", "배차 지연", "현재 자동배차에서 확정 가능한 기사를 찾지 못했습니다.", order.getOrderId());
            } else {
                launchNextWave(job.getDispatchJobId());
            }
        }

        return new DispatchOfferDecisionResponse(
                offer.getDispatchOfferId(),
                job.getDispatchJobId(),
                order.getOrderId(),
                false,
                job.getStatus().name(),
                order.getStatus()
        );
    }

    @Transactional
    public DispatchJob forceMatch(Long dispatchJobId, Long driverUserId) {
        DispatchJob job = dispatchJobRepository.findByIdForUpdate(dispatchJobId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found"));
        if (!job.isActive()) {
            throw new IllegalStateException("dispatch job already closed");
        }
        Order order = getDispatchableOrder(job.getOrderId());

        cancelOtherOpenOffers(job, null, "admin_force_match");
        job.markMatched(driverUserId);
        dispatchJobRepository.save(job);

        order.assignDriver(driverUserId, "ACCEPTED");
        order.getDriverList().clear();
        orderRepository.save(order);
        availabilityService.markMatched(driverUserId, order.getOrderId(), order.getStatus());

        sendNotificationSafely(order.getUser().getUserId(), "ORDER", "배차 확정", "관리자 개입으로 기사 배정이 확정되었습니다.", order.getOrderId());
        sendNotificationSafely(driverUserId, "ORDER", "배차 확정", "관리자 개입으로 회원님에게 오더가 배정되었습니다.", order.getOrderId());
        return job;
    }

    @Transactional
    public void processExpiredWaitingJobs() {
        List<DispatchJob> expiredJobs = dispatchJobRepository.findByStatusInAndExpiresAtBeforeAndActiveTrue(
                List.of(DispatchJobStatus.WAITING_RESPONSE),
                LocalDateTime.now()
        );
        for (DispatchJob job : expiredJobs) {
            try {
                advanceExpiredJob(job.getDispatchJobId());
            } catch (Exception e) {
                log.warn("failed to advance expired dispatch job. jobId={}, reason={}", job.getDispatchJobId(), e.getMessage());
            }
        }
    }

    @Transactional
    public DispatchJob advanceExpiredJob(Long dispatchJobId) {
        DispatchJob job = dispatchJobRepository.findByIdForUpdate(dispatchJobId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found"));
        if (!job.isActive() || job.getStatus() != DispatchJobStatus.WAITING_RESPONSE) {
            return job;
        }
        expireOpenOffers(job);
        if (job.getWave() >= maxWave()) {
            job.markFailed("NO_RESPONSE", "모든 웨이브가 만료되었습니다.");
            dispatchJobRepository.save(job);
            return job;
        }
        return launchNextWave(job.getDispatchJobId());
    }

    @Transactional
    public DispatchJob launchNextWave(Long dispatchJobId) {
        DispatchJob job = dispatchJobRepository.findByIdForUpdate(dispatchJobId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found"));
        Order order = getDispatchableOrder(job.getOrderId());
        if (!job.isActive()) {
            return job;
        }
        if (!"REQUESTED".equalsIgnoreCase(order.getStatus())) {
            closeJob(job, "ORDER_NOT_REQUESTED", "주문 상태가 배차 대기 상태가 아닙니다.");
            return job;
        }

        job.markSearching();
        dispatchJobRepository.save(job);

        List<Long> excludedDriverIds = dispatchOfferRepository.findByJobOrderByWaveAscRankAsc(job).stream()
                .map(DispatchOffer::getDriverUserId)
                .distinct()
                .toList();
        List<ScoredDriverCandidate> candidates = dispatchScoringService.scoreCandidates(order, job, excludedDriverIds);
        int nextWave = Math.max(1, job.getWave() + 1);

        if (candidates.isEmpty()) {
            job.markFailed("NO_ELIGIBLE_DRIVER", "조건을 만족하는 기사 후보를 찾지 못했습니다.");
            dispatchJobRepository.save(job);
            return job;
        }

        List<ScoredDriverCandidate> selectedCandidates = candidates.stream().limit(waveSize(nextWave)).toList();
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(waveTimeoutSeconds(nextWave));
        job.markOffering();
        dispatchJobRepository.save(job);

        for (int i = 0; i < selectedCandidates.size(); i++) {
            ScoredDriverCandidate candidate = selectedCandidates.get(i);
            DispatchOffer offer = dispatchOfferRepository.save(DispatchOffer.builder()
                    .job(job)
                    .orderId(order.getOrderId())
                    .driverUserId(candidate.user().getUserId())
                    .wave(nextWave)
                    .rank(i + 1)
                    .score(candidate.score())
                    .distanceKm(scaleDistance(candidate.distanceKm()))
                    .etaMinutes(candidate.etaMinutes())
                    .status(DispatchOfferStatus.PENDING)
                    .scoreBreakdownJson(toJson(Map.of(
                            "reasons", candidate.reasons(),
                            "penalties", candidate.penalties()
                    )))
                    .candidateSnapshotJson(toJson(buildCandidateSnapshot(candidate)))
                    .expireAt(expireAt)
                    .build());
            offer.markPushSent();
            dispatchOfferRepository.save(offer);
            sendOfferNotification(order, candidate.driver(), offer);
        }

        job.markWaitingResponse(nextWave, selectedCandidates.size(), expireAt, toJson(buildWaveDebugSnapshot(selectedCandidates)));
        dispatchJobRepository.save(job);
        return job;
    }

    private Order getDispatchableOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));
        if (order.getDriverNo() != null && !"REQUESTED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("order already has assigned driver");
        }
        return order;
    }

    private void closeJob(DispatchJob job, String reasonCode, String reasonMessage) {
        cancelOtherOpenOffers(job, null, "job_closed");
        job.cancel(reasonCode, reasonMessage);
        dispatchJobRepository.save(job);
    }

    private void cancelOtherOpenOffers(DispatchJob job, Long keepOfferId, String reason) {
        List<DispatchOffer> openOffers = dispatchOfferRepository.findByJobAndStatusIn(job, OPEN_OFFER_STATUSES);
        for (DispatchOffer openOffer : openOffers) {
            if (keepOfferId != null && keepOfferId.equals(openOffer.getDispatchOfferId())) {
                continue;
            }
            openOffer.cancel(reason);
        }
        dispatchOfferRepository.saveAll(openOffers);
    }

    private void expireOpenOffers(DispatchJob job) {
        List<DispatchOffer> openOffers = dispatchOfferRepository.findByJobAndStatusIn(job, OPEN_OFFER_STATUSES);
        for (DispatchOffer offer : openOffers) {
            offer.markExpired();
        }
        dispatchOfferRepository.saveAll(openOffers);
    }

    private boolean hasOpenOffers(DispatchJob job) {
        return !dispatchOfferRepository.findByJobAndStatusIn(job, OPEN_OFFER_STATUSES).isEmpty();
    }

    private DispatchPriority resolvePriority(Order order) {
        OrderSnapshot snapshot = order.getSnapshot();
        if (snapshot != null && snapshot.isInstant()) {
            return DispatchPriority.HIGH;
        }
        return DispatchPriority.NORMAL;
    }

    private void ensureAutoDispatchUnlocked(Order order) {
        OrderSnapshot snapshot = order == null ? null : order.getSnapshot();
        if (snapshot != null && snapshot.isAutoDispatchLocked()) {
            throw new IllegalStateException("order auto dispatch is locked");
        }
    }

    private int maxWave() {
        return 3;
    }

    private int waveSize(int wave) {
        return switch (wave) {
            case 1 -> 3;
            case 2 -> 5;
            default -> 7;
        };
    }

    private int waveTimeoutSeconds(int wave) {
        return switch (wave) {
            case 1 -> 20;
            case 2 -> 25;
            default -> 30;
        };
    }

    private Map<String, Object> buildCandidateSnapshot(ScoredDriverCandidate candidate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("driverUserId", candidate.user().getUserId());
        payload.put("driverId", candidate.driver().getDriverId());
        payload.put("nickname", candidate.user().getNickname());
        payload.put("carNum", candidate.driver().getCarNum());
        payload.put("carType", candidate.driver().getCarType());
        payload.put("tonnage", candidate.driver().getTonnage());
        payload.put("availability", candidate.snapshot().getAvailabilityStatus());
        payload.put("lastLocationAt", candidate.snapshot().getLastLocationAt());
        payload.put("score", candidate.score());
        payload.put("distanceKm", candidate.distanceKm());
        payload.put("etaMinutes", candidate.etaMinutes());
        return payload;
    }

    private List<Map<String, Object>> buildWaveDebugSnapshot(List<ScoredDriverCandidate> candidates) {
        return candidates.stream().limit(10).map(this::buildCandidateSnapshot).toList();
    }

    private void sendOfferNotification(Order order, Driver driver, DispatchOffer offer) {
        if (driver == null || driver.getUser() == null) {
            return;
        }
        String body = String.format(
                "상차 %s -> 하차 %s / 오퍼 응답 시간 %d초",
                trimAddress(order.getSnapshot() == null ? null : order.getSnapshot().getStartAddr()),
                trimAddress(order.getSnapshot() == null ? null : order.getSnapshot().getEndAddr()),
                waveTimeoutSeconds(offer.getWave())
        );
        sendNotificationSafely(driver.getUser().getUserId(), "ORDER", "새 배차 제안", body, order.getOrderId());
    }

    private void sendNotificationSafely(Long userId, String type, String title, String body, Long targetId) {
        try {
            notificationService.sendNotification(userId, type, title, body, targetId);
        } catch (Exception e) {
            log.warn("dispatch notification failed. targetId={}, reason={}", targetId, e.getMessage());
        }
    }

    private String trimAddress(String address) {
        if (address == null || address.isBlank()) {
            return "-";
        }
        return address.length() <= 18 ? address : address.substring(0, 18);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String defaultMessage(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private BigDecimal scaleDistance(BigDecimal distanceKm) {
        return distanceKm == null ? null : distanceKm.setScale(3, RoundingMode.HALF_UP);
    }
}
