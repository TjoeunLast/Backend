package com.example.project.domain.dispatch.service.query;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.DispatchOffer;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobStatus;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchOfferStatus;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchJobListItemResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchStatusResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DriverDispatchOfferResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.JobSnapshot;
import com.example.project.domain.dispatch.dto.DispatchDtos.MatchedDriver;
import com.example.project.domain.dispatch.dto.DispatchDtos.OfferSnapshot;
import com.example.project.domain.dispatch.dto.DispatchDtos.Summary;
import com.example.project.domain.dispatch.repository.DispatchJobRepository;
import com.example.project.domain.dispatch.repository.DispatchOfferRepository;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.DriverRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DispatchQueryService {

    private static final List<DispatchOfferStatus> DRIVER_VISIBLE_OFFER_STATUSES = List.of(
            DispatchOfferStatus.PENDING,
            DispatchOfferStatus.PUSH_SENT,
            DispatchOfferStatus.OPENED
    );

    private final DispatchJobRepository dispatchJobRepository;
    private final DispatchOfferRepository dispatchOfferRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;

    @Transactional(readOnly = true)
    public DispatchStatusResponse getOrderStatus(Long orderId, Users currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));
        DispatchJob latestJob = dispatchJobRepository.findFirstByOrderIdAndActiveTrueOrderByStartedAtDesc(orderId)
                .orElseGet(() -> dispatchJobRepository.findByOrderIdOrderByStartedAtDesc(orderId).stream().findFirst().orElse(null));
        ensureCanViewOrderDispatch(order, latestJob, currentUser);
        return buildDispatchStatus(order, latestJob);
    }

    @Transactional(readOnly = true)
    public DispatchStatusResponse getJobStatus(Long dispatchJobId, Users currentUser) {
        DispatchJob job = dispatchJobRepository.findById(dispatchJobId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch job not found. jobId=" + dispatchJobId));
        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + job.getOrderId()));
        ensureCanViewOrderDispatch(order, job, currentUser);
        return buildDispatchStatus(order, job);
    }

    @Transactional(readOnly = true)
    public List<DispatchJobListItemResponse> getRecentJobs() {
        return dispatchJobRepository.findTop50ByOrderByStartedAtDesc().stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public List<DriverDispatchOfferResponse> getDriverOpenOffers(Long driverUserId) {
        LocalDateTime now = LocalDateTime.now();
        List<DispatchOffer> offers = dispatchOfferRepository.findByDriverUserIdAndStatusInOrderByExpireAtAsc(
                driverUserId,
                DRIVER_VISIBLE_OFFER_STATUSES
        );

        boolean dirty = false;
        for (DispatchOffer offer : offers) {
            if (offer.getExpireAt() != null && offer.getExpireAt().isBefore(now)) {
                offer.markExpired();
                dirty = true;
                continue;
            }
            if (offer.getStatus() == DispatchOfferStatus.PUSH_SENT) {
                offer.markOpened();
                dirty = true;
            }
        }
        if (dirty) {
            dispatchOfferRepository.saveAll(offers);
        }

        return offers.stream()
                .filter(DispatchOffer::isOpen)
                .filter(offer -> offer.getExpireAt() == null || offer.getExpireAt().isAfter(now))
                .map(this::toDriverOfferResponse)
                .toList();
    }

    private DispatchStatusResponse buildDispatchStatus(Order order, DispatchJob job) {
        List<DispatchOffer> offers = job == null
                ? List.of()
                : dispatchOfferRepository.findByJobOrderByWaveAscRankAsc(job);

        Long matchedDriverUserId = job != null && job.getMatchedDriverUserId() != null
                ? job.getMatchedDriverUserId()
                : order.getDriverNo();

        return DispatchStatusResponse.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getStatus())
                .dispatchPublicStatus(resolvePublicStatus(order, job))
                .job(toJobSnapshot(job))
                .summary(toSummary(offers))
                .matchedDriver(toMatchedDriver(matchedDriverUserId))
                .offers(offers.stream().map(this::toOfferSnapshot).toList())
                .build();
    }

    private void ensureCanViewOrderDispatch(Order order, DispatchJob job, Users currentUser) {
        if (currentUser == null) {
            throw new IllegalStateException("authentication required");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (order.getUser() != null && Objects.equals(order.getUser().getUserId(), currentUser.getUserId())) {
            return;
        }
        if (Objects.equals(order.getDriverNo(), currentUser.getUserId())) {
            return;
        }
        if (job != null) {
            boolean hasOffer = dispatchOfferRepository.findByJobOrderByWaveAscRankAsc(job).stream()
                    .anyMatch(offer -> Objects.equals(offer.getDriverUserId(), currentUser.getUserId()));
            if (hasOffer) {
                return;
            }
        }
        throw new IllegalStateException("dispatch access denied");
    }

    private String resolvePublicStatus(Order order, DispatchJob job) {
        if (job == null) {
            if (order.getDriverNo() != null) {
                return "MATCHED";
            }
            if ("REQUESTED".equalsIgnoreCase(order.getStatus())) {
                return "IDLE";
            }
            return order.getStatus();
        }
        return switch (job.getStatus()) {
            case QUEUED, SEARCHING, OFFERING, WAITING_RESPONSE -> "SEARCHING";
            case MATCHED -> "MATCHED";
            case FAILED -> "FAILED";
            case CANCELLED -> "CANCELLED";
        };
    }

    private JobSnapshot toJobSnapshot(DispatchJob job) {
        if (job == null) {
            return null;
        }
        return JobSnapshot.builder()
                .dispatchJobId(job.getDispatchJobId())
                .jobType(job.getJobType() == null ? null : job.getJobType().name())
                .status(job.getStatus() == null ? null : job.getStatus().name())
                .wave(job.getWave())
                .dispatchMode(job.getDispatchMode() == null ? null : job.getDispatchMode().name())
                .dispatchPriority(job.getDispatchPriority() == null ? null : job.getDispatchPriority().name())
                .candidateCount(job.getCurrentCandidateCount())
                .matchedDriverUserId(job.getMatchedDriverUserId())
                .failureReasonCode(job.getFailureReasonCode())
                .failureReasonMessage(job.getFailureReasonMessage())
                .startedAt(job.getStartedAt())
                .lastWaveStartedAt(job.getLastWaveStartedAt())
                .expiresAt(job.getExpiresAt())
                .closedAt(job.getClosedAt())
                .build();
    }

    private Summary toSummary(List<DispatchOffer> offers) {
        int offersSent = offers.size();
        int offersOpen = (int) offers.stream().filter(DispatchOffer::isOpen).count();
        int offersRejected = countByStatus(offers, DispatchOfferStatus.REJECTED);
        int offersExpired = countByStatus(offers, DispatchOfferStatus.EXPIRED);
        int offersCancelled = countByStatus(offers, DispatchOfferStatus.CANCELLED);
        return Summary.builder()
                .offersSent(offersSent)
                .offersOpen(offersOpen)
                .offersRejected(offersRejected)
                .offersExpired(offersExpired)
                .offersCancelled(offersCancelled)
                .build();
    }

    private int countByStatus(List<DispatchOffer> offers, DispatchOfferStatus status) {
        return (int) offers.stream().filter(offer -> offer.getStatus() == status).count();
    }

    private MatchedDriver toMatchedDriver(Long driverUserId) {
        if (driverUserId == null) {
            return null;
        }
        Driver driver = driverRepository.findByUser_UserId(driverUserId).orElse(null);
        if (driver == null || driver.getUser() == null) {
            return null;
        }
        return MatchedDriver.builder()
                .driverUserId(driverUserId)
                .driverId(driver.getDriverId())
                .nickname(driver.getUser().getNickname())
                .carNum(driver.getCarNum())
                .carType(driver.getCarType())
                .tonnage(driver.getTonnage())
                .build();
    }

    private OfferSnapshot toOfferSnapshot(DispatchOffer offer) {
        return OfferSnapshot.builder()
                .offerId(offer.getDispatchOfferId())
                .driverUserId(offer.getDriverUserId())
                .wave(offer.getWave())
                .rank(offer.getRank())
                .status(offer.getStatus().name())
                .score(offer.getScore())
                .distanceKm(offer.getDistanceKm())
                .etaMinutes(offer.getEtaMinutes())
                .sentAt(offer.getSentAt())
                .expireAt(offer.getExpireAt())
                .respondedAt(offer.getRespondedAt())
                .rejectReasonCode(offer.getRejectReasonCode())
                .closedReason(offer.getClosedReason())
                .scoreBreakdownJson(offer.getScoreBreakdownJson())
                .build();
    }

    private DriverDispatchOfferResponse toDriverOfferResponse(DispatchOffer offer) {
        Order order = orderRepository.findById(offer.getOrderId()).orElse(null);
        return new DriverDispatchOfferResponse(
                offer.getDispatchOfferId(),
                offer.getJob().getDispatchJobId(),
                offer.getOrderId(),
                order == null ? "UNKNOWN" : order.getStatus(),
                offer.getStatus().name(),
                offer.getWave(),
                offer.getRank(),
                offer.getScore(),
                offer.getDistanceKm(),
                offer.getEtaMinutes(),
                offer.getExpireAt(),
                offer.getScoreBreakdownJson()
        );
    }

    private DispatchJobListItemResponse toListItem(DispatchJob job) {
        return new DispatchJobListItemResponse(
                job.getDispatchJobId(),
                job.getOrderId(),
                job.getJobType() == null ? null : job.getJobType().name(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getWave(),
                job.getFailureReasonCode(),
                job.getStartedAt(),
                job.getExpiresAt(),
                job.getClosedAt()
        );
    }
}
