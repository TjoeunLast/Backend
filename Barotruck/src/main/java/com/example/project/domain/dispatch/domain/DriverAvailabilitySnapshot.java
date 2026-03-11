package com.example.project.domain.dispatch.domain;

import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DriverAvailabilityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DRIVER_AVAILABILITY_SNAPSHOT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAvailabilitySnapshot {

    @Id
    @Column(name = "DRIVER_USER_ID")
    private Long driverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "AVAILABILITY_STATUS", nullable = false, length = 20)
    private DriverAvailabilityStatus availabilityStatus;

    @Column(name = "CURRENT_LAT", precision = 18, scale = 10)
    private BigDecimal currentLat;

    @Column(name = "CURRENT_LNG", precision = 18, scale = 10)
    private BigDecimal currentLng;

    @Column(name = "LAST_LOCATION_AT")
    private LocalDateTime lastLocationAt;

    @Column(name = "LAST_ACTIVE_AT")
    private LocalDateTime lastActiveAt;

    @Column(name = "VEHICLE_TYPE", length = 50)
    private String vehicleType;

    @Column(name = "TONNAGE", precision = 10, scale = 2)
    private BigDecimal tonnage;

    @Lob
    @Column(name = "OPERATING_REGIONS_JSON")
    private String operatingRegionsJson;

    @Builder.Default
    @Column(name = "DOCUMENT_VERIFIED", nullable = false)
    private Boolean documentVerified = true;

    @Builder.Default
    @Column(name = "DISPATCH_BLOCKED", nullable = false)
    private Boolean dispatchBlocked = false;

    @Column(name = "DISPATCH_BLOCK_REASON", length = 200)
    private String dispatchBlockReason;

    @Column(name = "ACTIVE_ORDER_ID")
    private Long activeOrderId;

    @Column(name = "ACTIVE_ORDER_STATUS", length = 30)
    private String activeOrderStatus;

    @Builder.Default
    @Column(name = "RECENT_DISPATCH_COUNT", nullable = false)
    private Integer recentDispatchCount = 0;

    @Column(name = "LAST_MATCHED_AT")
    private LocalDateTime lastMatchedAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public void applyAvailabilityStatus(DriverAvailabilityStatus status) {
        this.availabilityStatus = status;
        this.lastActiveAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void applyLocation(BigDecimal lat, BigDecimal lng, LocalDateTime recordedAt) {
        this.currentLat = lat;
        this.currentLng = lng;
        this.lastLocationAt = recordedAt == null ? LocalDateTime.now() : recordedAt;
        this.lastActiveAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void applyDriverProfile(String vehicleType, BigDecimal tonnage, boolean blocked, String blockReason) {
        this.vehicleType = vehicleType;
        this.tonnage = tonnage;
        this.dispatchBlocked = blocked;
        this.dispatchBlockReason = blockReason;
        this.documentVerified = !blocked;
        this.updatedAt = LocalDateTime.now();
    }

    public void markMatched(Long orderId, String orderStatus) {
        this.availabilityStatus = DriverAvailabilityStatus.BUSY;
        this.activeOrderId = orderId;
        this.activeOrderStatus = orderStatus;
        this.lastMatchedAt = LocalDateTime.now();
        this.recentDispatchCount = (recentDispatchCount == null ? 0 : recentDispatchCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void refreshBusyOrder(Long orderId, String orderStatus) {
        this.availabilityStatus = DriverAvailabilityStatus.BUSY;
        this.activeOrderId = orderId;
        this.activeOrderStatus = orderStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void markOrderReleased() {
        this.activeOrderId = null;
        this.activeOrderStatus = null;
        if (this.availabilityStatus == DriverAvailabilityStatus.BUSY) {
            this.availabilityStatus = DriverAvailabilityStatus.ONLINE;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void markBlocked(String reason) {
        this.availabilityStatus = DriverAvailabilityStatus.BLOCKED;
        this.dispatchBlocked = true;
        this.dispatchBlockReason = reason;
        this.documentVerified = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void markOnlineIfPossible() {
        if (!Boolean.TRUE.equals(dispatchBlocked)) {
            this.availabilityStatus = DriverAvailabilityStatus.ONLINE;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
