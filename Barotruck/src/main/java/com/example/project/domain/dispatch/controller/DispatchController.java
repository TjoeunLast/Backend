package com.example.project.domain.dispatch.controller;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.DriverAvailabilitySnapshot;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobType;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchOfferDecisionResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchOfferRejectRequest;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchRetryRequest;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchRunRequest;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchStatusResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DispatchJobListItemResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DriverAvailabilityUpdateRequest;
import com.example.project.domain.dispatch.dto.DispatchDtos.DriverDispatchOfferResponse;
import com.example.project.domain.dispatch.dto.DispatchDtos.DriverLocationUpdateRequest;
import com.example.project.domain.dispatch.service.availability.DispatchAvailabilityService;
import com.example.project.domain.dispatch.service.core.DispatchOrchestratorService;
import com.example.project.domain.dispatch.service.query.DispatchQueryService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchOrchestratorService dispatchOrchestratorService;
    private final DispatchQueryService dispatchQueryService;
    private final DispatchAvailabilityService dispatchAvailabilityService;
    private final OrderRepository orderRepository;

    @PostMapping("/orders/{orderId}/run")
    @PreAuthorize("hasAnyRole('ADMIN','SHIPPER')")
    public ResponseEntity<DispatchStatusResponse> runDispatch(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Users currentUser,
            @RequestBody(required = false) DispatchRunRequest request) {
        ensureOrderOwnerOrAdmin(orderId, currentUser);
        Boolean forceRebuild = request != null ? request.forceRebuild() : null;
        String reason = request != null ? request.reason() : null;
        DispatchJob job = dispatchOrchestratorService.startAutoDispatch(orderId, Boolean.TRUE.equals(forceRebuild), reason);
        return ResponseEntity.ok(dispatchQueryService.getJobStatus(job.getDispatchJobId(), currentUser));
    }

    @PostMapping("/orders/{orderId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN','SHIPPER')")
    public ResponseEntity<DispatchStatusResponse> retryDispatch(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Users currentUser,
            @RequestBody(required = false) DispatchRetryRequest request) {
        ensureOrderOwnerOrAdmin(orderId, currentUser);
        DispatchJobType jobType = parseJobType(request == null ? null : request.jobType());
        DispatchJob job = dispatchOrchestratorService.retryDispatch(orderId, jobType, request == null ? null : request.reasonCode());
        return ResponseEntity.ok(dispatchQueryService.getJobStatus(job.getDispatchJobId(), currentUser));
    }

    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SHIPPER','DRIVER')")
    public ResponseEntity<DispatchStatusResponse> getOrderDispatchStatus(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Users currentUser) {
        return ResponseEntity.ok(dispatchQueryService.getOrderStatus(orderId, currentUser));
    }

    @GetMapping("/jobs/{dispatchJobId}")
    public ResponseEntity<DispatchStatusResponse> getJobDispatchStatus(
            @PathVariable Long dispatchJobId,
            @AuthenticationPrincipal Users currentUser) {
        return ResponseEntity.ok(dispatchQueryService.getJobStatus(dispatchJobId, currentUser));
    }

    @GetMapping("/jobs/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DispatchJobListItemResponse>> getRecentJobs() {
        return ResponseEntity.ok(dispatchQueryService.getRecentJobs());
    }

    @PostMapping("/jobs/{dispatchJobId}/force-match")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DispatchStatusResponse> forceMatch(
            @PathVariable Long dispatchJobId,
            @RequestParam("driverUserId") Long driverUserId,
            @AuthenticationPrincipal Users currentUser) {
        DispatchJob job = dispatchOrchestratorService.forceMatch(dispatchJobId, driverUserId);
        return ResponseEntity.ok(dispatchQueryService.getJobStatus(job.getDispatchJobId(), currentUser));
    }

    @PatchMapping("/driver/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> updateAvailability(
            @AuthenticationPrincipal Users currentUser,
            @RequestBody DriverAvailabilityUpdateRequest request) {
        DriverAvailabilitySnapshot snapshot = dispatchAvailabilityService.updateAvailability(
                currentUser.getUserId(),
                request == null ? null : request.availabilityStatus()
        );
        return ResponseEntity.ok(toAvailabilityPayload(snapshot));
    }

    @PatchMapping("/driver/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @AuthenticationPrincipal Users currentUser,
            @RequestBody DriverLocationUpdateRequest request) {
        DriverAvailabilitySnapshot snapshot = dispatchAvailabilityService.updateLocation(
                currentUser.getUserId(),
                request == null ? null : request.lat(),
                request == null ? null : request.lng(),
                request == null ? null : request.recordedAt()
        );
        return ResponseEntity.ok(toAvailabilityPayload(snapshot));
    }

    @GetMapping("/driver/offers")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<DriverDispatchOfferResponse>> getDriverOffers(@AuthenticationPrincipal Users currentUser) {
        return ResponseEntity.ok(dispatchQueryService.getDriverOpenOffers(currentUser.getUserId()));
    }

    @PostMapping("/offers/{offerId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DispatchOfferDecisionResponse> acceptOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal Users currentUser) {
        return ResponseEntity.ok(dispatchOrchestratorService.acceptOffer(offerId, currentUser.getUserId()));
    }

    @PostMapping("/offers/{offerId}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DispatchOfferDecisionResponse> rejectOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal Users currentUser,
            @RequestBody(required = false) DispatchOfferRejectRequest request) {
        String reasonCode = request == null ? null : request.reasonCode();
        return ResponseEntity.ok(dispatchOrchestratorService.rejectOffer(offerId, currentUser.getUserId(), reasonCode));
    }

    private DispatchJobType parseJobType(String rawJobType) {
        if (rawJobType == null || rawJobType.isBlank()) {
            return DispatchJobType.REASSIGN;
        }
        try {
            return DispatchJobType.valueOf(rawJobType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported dispatch job type: " + rawJobType);
        }
    }

    private void ensureOrderOwnerOrAdmin(Long orderId, Users currentUser) {
        if (currentUser == null) {
            throw new IllegalStateException("authentication required");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));
        if (order.getUser() != null && Objects.equals(order.getUser().getUserId(), currentUser.getUserId())) {
            return;
        }
        throw new IllegalStateException("dispatch control access denied");
    }

    private Map<String, Object> toAvailabilityPayload(DriverAvailabilitySnapshot snapshot) {
        return Map.of(
                "driverUserId", snapshot.getDriverUserId(),
                "availabilityStatus", snapshot.getAvailabilityStatus().name(),
                "activeOrderId", snapshot.getActiveOrderId() == null ? 0L : snapshot.getActiveOrderId(),
                "activeOrderStatus", snapshot.getActiveOrderStatus() == null ? "" : snapshot.getActiveOrderStatus(),
                "updatedAt", snapshot.getUpdatedAt() == null ? "" : snapshot.getUpdatedAt().toString()
        );
    }
}
