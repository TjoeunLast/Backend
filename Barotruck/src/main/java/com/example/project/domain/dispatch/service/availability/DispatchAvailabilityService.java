package com.example.project.domain.dispatch.service.availability;

import com.example.project.domain.dispatch.domain.DriverAvailabilitySnapshot;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DriverAvailabilityStatus;
import com.example.project.domain.dispatch.repository.DriverAvailabilitySnapshotRepository;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import com.example.project.member.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DispatchAvailabilityService {

    private static final List<String> BUSY_ORDER_STATUSES = List.of("ALLOCATED", "ACCEPTED", "LOADING", "IN_TRANSIT", "UNLOADING");

    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final DriverAvailabilitySnapshotRepository snapshotRepository;

    public DriverAvailabilitySnapshot getOrCreateSnapshot(Driver driver) {
        if (driver == null || driver.getUser() == null || driver.getUser().getUserId() == null) {
            throw new IllegalArgumentException("driver user id is required");
        }
        Long driverUserId = driver.getUser().getUserId();
        return snapshotRepository.findById(driverUserId)
                .map(snapshot -> syncSnapshot(snapshot, driver))
                .orElseGet(() -> snapshotRepository.save(buildDefaultSnapshot(driver)));
    }

    public DriverAvailabilitySnapshot updateAvailability(Long driverUserId, String rawStatus) {
        Driver driver = driverRepository.findByUser_UserId(driverUserId)
                .orElseThrow(() -> new IllegalArgumentException("driver profile not found. userId=" + driverUserId));
        DriverAvailabilitySnapshot snapshot = getOrCreateSnapshot(driver);
        DriverAvailabilityStatus status = normalizeStatus(rawStatus);
        if (isBlockedDriver(driver)) {
            snapshot.markBlocked(resolveBlockReason(driver));
        } else {
            snapshot.applyAvailabilityStatus(status);
            syncBusyState(snapshot, driverUserId);
        }
        return snapshotRepository.save(snapshot);
    }

    public DriverAvailabilitySnapshot updateLocation(Long driverUserId, Double lat, Double lng, String recordedAt) {
        Driver driver = driverRepository.findByUser_UserId(driverUserId)
                .orElseThrow(() -> new IllegalArgumentException("driver profile not found. userId=" + driverUserId));
        DriverAvailabilitySnapshot snapshot = getOrCreateSnapshot(driver);
        BigDecimal latitude = lat == null ? null : BigDecimal.valueOf(lat);
        BigDecimal longitude = lng == null ? null : BigDecimal.valueOf(lng);
        snapshot.applyLocation(latitude, longitude, parseDateTime(recordedAt));
        syncBusyState(snapshot, driverUserId);
        return snapshotRepository.save(snapshot);
    }

    public void markMatched(Long driverUserId, Long orderId, String orderStatus) {
        if (driverUserId == null) {
            return;
        }
        Driver driver = driverRepository.findByUser_UserId(driverUserId).orElse(null);
        if (driver == null) {
            return;
        }
        DriverAvailabilitySnapshot snapshot = getOrCreateSnapshot(driver);
        snapshot.markMatched(orderId, orderStatus);
        snapshotRepository.save(snapshot);
    }

    public void syncBusyOrder(Long driverUserId, Long orderId, String orderStatus) {
        if (driverUserId == null) {
            return;
        }
        Driver driver = driverRepository.findByUser_UserId(driverUserId).orElse(null);
        if (driver == null) {
            return;
        }
        DriverAvailabilitySnapshot snapshot = getOrCreateSnapshot(driver);
        snapshot.refreshBusyOrder(orderId, orderStatus);
        snapshotRepository.save(snapshot);
    }

    public void markOrderReleased(Long driverUserId, Long orderId) {
        if (driverUserId == null) {
            return;
        }
        DriverAvailabilitySnapshot snapshot = snapshotRepository.findById(driverUserId).orElse(null);
        if (snapshot == null) {
            return;
        }
        if (orderId == null || orderId.equals(snapshot.getActiveOrderId())) {
            snapshot.markOrderReleased();
            syncBusyState(snapshot, driverUserId);
            snapshotRepository.save(snapshot);
        }
    }

    @Transactional(readOnly = true)
    public List<DriverAvailabilitySnapshot> findOnlineSnapshots() {
        return snapshotRepository.findByAvailabilityStatus(DriverAvailabilityStatus.ONLINE);
    }

    private DriverAvailabilitySnapshot buildDefaultSnapshot(Driver driver) {
        DriverAvailabilitySnapshot snapshot = DriverAvailabilitySnapshot.builder()
                .driverUserId(driver.getUser().getUserId())
                .availabilityStatus(DriverAvailabilityStatus.ONLINE)
                .currentLat(toDecimal(driver.getLat()))
                .currentLng(toDecimal(driver.getLng()))
                .lastLocationAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .vehicleType(driver.getCarType())
                .tonnage(driver.getTonnage())
                .documentVerified(true)
                .dispatchBlocked(false)
                .recentDispatchCount(0)
                .updatedAt(LocalDateTime.now())
                .build();
        if (isBlockedDriver(driver)) {
            snapshot.markBlocked(resolveBlockReason(driver));
        } else {
            syncBusyState(snapshot, driver.getUser().getUserId());
        }
        return snapshot;
    }

    private DriverAvailabilitySnapshot syncSnapshot(DriverAvailabilitySnapshot snapshot, Driver driver) {
        snapshot.applyDriverProfile(driver.getCarType(), driver.getTonnage(), isBlockedDriver(driver), resolveBlockReason(driver));
        if (snapshot.getCurrentLat() == null && driver.getLat() != null) {
            snapshot.applyLocation(BigDecimal.valueOf(driver.getLat()), snapshot.getCurrentLng(), LocalDateTime.now());
        }
        if (snapshot.getCurrentLng() == null && driver.getLng() != null) {
            snapshot.applyLocation(snapshot.getCurrentLat(), BigDecimal.valueOf(driver.getLng()), LocalDateTime.now());
        }
        syncBusyState(snapshot, driver.getUser().getUserId());
        return snapshotRepository.save(snapshot);
    }

    private void syncBusyState(DriverAvailabilitySnapshot snapshot, Long driverUserId) {
        List<Order> activeOrders = orderRepository.findByDriverNoAndStatusIn(driverUserId, BUSY_ORDER_STATUSES);
        if (!activeOrders.isEmpty()) {
            Order latest = activeOrders.get(0);
            snapshot.markMatched(latest.getOrderId(), latest.getStatus());
            return;
        }
        if (snapshot.getAvailabilityStatus() == DriverAvailabilityStatus.BUSY) {
            snapshot.markOrderReleased();
        }
        if (snapshot.getAvailabilityStatus() == null) {
            snapshot.markOnlineIfPossible();
        }
    }

    private boolean isBlockedDriver(Driver driver) {
        return driver == null
                || driver.getUser() == null
                || "Y".equalsIgnoreCase(driver.getIsSuspended());
    }

    private String resolveBlockReason(Driver driver) {
        if (driver == null || driver.getUser() == null) {
            return "driver_profile_missing";
        }
        if ("Y".equalsIgnoreCase(driver.getIsSuspended())) {
            return "driver_suspended";
        }
        return null;
    }

    private DriverAvailabilityStatus normalizeStatus(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return DriverAvailabilityStatus.ONLINE;
        }
        try {
            return DriverAvailabilityStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported availability status: " + rawStatus);
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(raw.trim()).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
    }

    private BigDecimal toDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
