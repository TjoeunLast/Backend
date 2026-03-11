package com.example.project.domain.dispatch.service.scoring;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.DriverAvailabilitySnapshot;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DriverAvailabilityStatus;
import com.example.project.domain.dispatch.service.availability.DispatchAvailabilityService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class DispatchScoringService {

    private static final Pattern TONNAGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final DateTimeFormatter ORDER_SCHEDULE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Duration LOCATION_FRESHNESS = Duration.ofMinutes(20);

    private final DriverRepository driverRepository;
    private final DispatchAvailabilityService availabilityService;

    public List<ScoredDriverCandidate> scoreCandidates(Order order, DispatchJob job, List<Long> excludedDriverUserIds) {
        List<Long> excludedIds = excludedDriverUserIds == null ? List.of() : excludedDriverUserIds;
        return driverRepository.findAll().stream()
                .map(driver -> scoreCandidate(order, driver, excludedIds))
                .filter(candidate -> candidate != null && candidate.eligible())
                .sorted(Comparator.comparing(ScoredDriverCandidate::score).reversed())
                .toList();
    }

    private ScoredDriverCandidate scoreCandidate(Order order, Driver driver, List<Long> excludedDriverUserIds) {
        if (order == null || driver == null || driver.getUser() == null) {
            return null;
        }
        Users user = driver.getUser();
        Long driverUserId = user.getUserId();
        if (driverUserId == null || excludedDriverUserIds.contains(driverUserId)) {
            return null;
        }
        OrderSnapshot snapshot = order.getSnapshot();
        if (snapshot == null) {
            return null;
        }

        DriverAvailabilitySnapshot availability = availabilityService.getOrCreateSnapshot(driver);
        List<String> rejectReasons = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> penalties = new ArrayList<>();
        BigDecimal score = BigDecimal.ZERO;

        if (availability.getAvailabilityStatus() != DriverAvailabilityStatus.ONLINE) {
            rejectReasons.add("availability:" + availability.getAvailabilityStatus());
        }
        if (!user.isAutoDispatchEnabled()) {
            rejectReasons.add("auto_dispatch_disabled");
        }
        if (Boolean.TRUE.equals(availability.getDispatchBlocked())) {
            rejectReasons.add("dispatch_blocked:" + safeText(availability.getDispatchBlockReason()));
        }
        if (!Boolean.TRUE.equals(availability.getDocumentVerified())) {
            rejectReasons.add("document_unverified");
        }
        if (!vehicleMatches(snapshot, driver)) {
            rejectReasons.add("vehicle_mismatch");
        }
        if (!isScheduleAvailable(snapshot.getStartSchedule())) {
            rejectReasons.add("schedule_unavailable");
        }
        if (!isLocationFresh(availability.getLastLocationAt())) {
            penalties.add("location_stale");
            score = score.subtract(new BigDecimal("8"));
        }

        if (!rejectReasons.isEmpty()) {
            return new ScoredDriverCandidate(user, driver, availability, BigDecimal.ZERO, null, null, reasons, penalties, rejectReasons);
        }

        if (matchesNeighborhood(snapshot, driver)) {
            score = score.add(new BigDecimal("20"));
            reasons.add("neighborhood_match");
        }

        BigDecimal distanceKm = calculateDistanceKm(
                availability.getCurrentLat(),
                availability.getCurrentLng(),
                snapshot.getStartLat(),
                snapshot.getStartLng()
        );
        Integer etaMinutes = estimateEtaMinutes(distanceKm);
        if (distanceKm != null) {
            if (distanceKm.compareTo(new BigDecimal("5")) <= 0) {
                score = score.add(new BigDecimal("30"));
                reasons.add("distance<=5km");
            } else if (distanceKm.compareTo(new BigDecimal("15")) <= 0) {
                score = score.add(new BigDecimal("22"));
                reasons.add("distance<=15km");
            } else if (distanceKm.compareTo(new BigDecimal("30")) <= 0) {
                score = score.add(new BigDecimal("14"));
                reasons.add("distance<=30km");
            } else if (distanceKm.compareTo(new BigDecimal("60")) <= 0) {
                score = score.add(new BigDecimal("6"));
                reasons.add("distance<=60km");
            } else {
                score = score.subtract(new BigDecimal("8"));
                penalties.add("distance>60km");
            }
        }

        if (etaMinutes != null) {
            if (etaMinutes <= 15) {
                score = score.add(new BigDecimal("18"));
                reasons.add("eta<=15m");
            } else if (etaMinutes <= 30) {
                score = score.add(new BigDecimal("12"));
                reasons.add("eta<=30m");
            } else if (etaMinutes <= 60) {
                score = score.add(new BigDecimal("5"));
                reasons.add("eta<=60m");
            } else {
                score = score.subtract(new BigDecimal("5"));
                penalties.add("eta>60m");
            }
        }

        BigDecimal requiredVehicleTonnage = parseRequestedVehicleTonnage(snapshot.getReqTonnage());
        if (driver.getTonnage() != null && requiredVehicleTonnage != null) {
            BigDecimal spare = driver.getTonnage().subtract(requiredVehicleTonnage);
            if (spare.compareTo(BigDecimal.ZERO) >= 0 && spare.compareTo(new BigDecimal("1.5")) <= 0) {
                score = score.add(new BigDecimal("15"));
                reasons.add("tonnage_close_fit");
            } else if (spare.compareTo(BigDecimal.ZERO) > 0) {
                score = score.add(new BigDecimal("8"));
                reasons.add("tonnage_spare_fit");
            }
        }

        long fareAmount = safeLong(snapshot.getBasePrice())
                + safeLong(snapshot.getLaborFee())
                + safeLong(snapshot.getPackagingPrice())
                + safeLong(snapshot.getInsuranceFee());
        if (fareAmount >= 300_000L) {
            score = score.add(new BigDecimal("8"));
            reasons.add("high_fare");
        } else if (fareAmount >= 120_000L) {
            score = score.add(new BigDecimal("4"));
            reasons.add("mid_fare");
        }

        Long ratingAvg = user.getRatingAvg();
        if (ratingAvg != null) {
            if (ratingAvg >= 45) {
                score = score.add(new BigDecimal("10"));
                reasons.add("rating_strong");
            } else if (ratingAvg >= 35) {
                score = score.add(new BigDecimal("5"));
                reasons.add("rating_ok");
            }
        }

        if (driver.getCareer() != null) {
            if (driver.getCareer() >= 5) {
                score = score.add(new BigDecimal("6"));
                reasons.add("career_5y_plus");
            } else if (driver.getCareer() >= 2) {
                score = score.add(new BigDecimal("3"));
                reasons.add("career_2y_plus");
            }
        }

        Integer recentDispatchCount = availability.getRecentDispatchCount();
        if (recentDispatchCount == null || recentDispatchCount == 0) {
            score = score.add(new BigDecimal("8"));
            reasons.add("fairness_boost_cold");
        } else if (recentDispatchCount <= 2) {
            score = score.add(new BigDecimal("4"));
            reasons.add("fairness_boost_light");
        } else if (recentDispatchCount >= 6) {
            score = score.subtract(new BigDecimal("4"));
            penalties.add("fairness_penalty_heavy_recent_dispatch");
        }

        LocalDateTime startSchedule = parseSchedule(snapshot.getStartSchedule());
        if (startSchedule != null) {
            LocalDateTime now = LocalDateTime.now();
            if (startSchedule.isBefore(now.plusHours(3))) {
                score = score.add(new BigDecimal("10"));
                reasons.add("urgent_pickup_window");
            } else if (startSchedule.isBefore(now.plusHours(12))) {
                score = score.add(new BigDecimal("4"));
                reasons.add("near_pickup_window");
            }
        }

        if (snapshot.isInstant()) {
            score = score.add(new BigDecimal("6"));
            reasons.add("instant_dispatch");
        }

        return new ScoredDriverCandidate(
                user,
                driver,
                availability,
                score.setScale(2, RoundingMode.HALF_UP),
                distanceKm,
                etaMinutes,
                reasons,
                penalties,
                rejectReasons
        );
    }

    private boolean vehicleMatches(OrderSnapshot snapshot, Driver driver) {
        if (snapshot == null || driver == null) {
            return false;
        }
        String requiredCarType = normalizeCarType(snapshot.getReqCarType());
        String driverCarType = normalizeCarType(driver.getCarType());
        if (!requiredCarType.isBlank() && !driverCarType.isBlank() && !requiredCarType.equals(driverCarType)) {
            return false;
        }
        BigDecimal requiredVehicleTonnage = parseRequestedVehicleTonnage(snapshot.getReqTonnage());
        if (driver.getTonnage() != null && requiredVehicleTonnage != null && driver.getTonnage().compareTo(requiredVehicleTonnage) < 0) {
            return false;
        }
        if (driver.getTonnage() != null && snapshot.getTonnage() != null && driver.getTonnage().compareTo(snapshot.getTonnage()) < 0) {
            return false;
        }
        return true;
    }

    private boolean matchesNeighborhood(OrderSnapshot snapshot, Driver driver) {
        if (snapshot == null || driver == null || driver.getNbhId() == null) {
            return false;
        }
        return driver.getNbhId().equals(snapshot.getStartNbhId()) || driver.getNbhId().equals(snapshot.getEndNbhId());
    }

    private boolean isScheduleAvailable(String scheduleText) {
        LocalDateTime schedule = parseSchedule(scheduleText);
        return schedule == null || schedule.isAfter(LocalDateTime.now().minusMinutes(10));
    }

    private boolean isLocationFresh(LocalDateTime lastLocationAt) {
        return lastLocationAt != null && lastLocationAt.isAfter(LocalDateTime.now().minus(LOCATION_FRESHNESS));
    }

    private LocalDateTime parseSchedule(String startSchedule) {
        if (startSchedule == null || startSchedule.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(startSchedule.trim(), ORDER_SCHEDULE_FORMATTER);
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal parseRequestedVehicleTonnage(String reqTonnage) {
        if (reqTonnage == null || reqTonnage.isBlank()) {
            return null;
        }
        Matcher matcher = TONNAGE_PATTERN.matcher(reqTonnage);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeCarType(String carType) {
        if (carType == null) {
            return "";
        }
        String normalized = carType.replaceAll("[\\s_\\-]+", "").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WING", "WINGBODY", "윙", "윙바디" -> "WING";
            case "CARGO", "카고" -> "CARGO";
            case "TOP", "TOPCAR", "탑", "탑차" -> "TOP";
            case "REFRIGERATED", "냉장", "냉장탑", "냉장탑차" -> "REFRIGERATED";
            case "FREEZER", "냉동", "냉동탑", "냉동탑차" -> "FREEZER";
            case "LABO", "라보" -> "LABO";
            default -> normalized;
        };
    }

    private BigDecimal calculateDistanceKm(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return null;
        }
        double earthRadiusKm = 6371.0;
        double fromLatValue = fromLat.doubleValue();
        double fromLngValue = fromLng.doubleValue();
        double toLatValue = toLat.doubleValue();
        double toLngValue = toLng.doubleValue();
        double dLat = Math.toRadians(toLatValue - fromLatValue);
        double dLng = Math.toRadians(toLngValue - fromLngValue);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLatValue)) * Math.cos(Math.toRadians(toLatValue))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(3, RoundingMode.HALF_UP);
    }

    private Integer estimateEtaMinutes(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        double avgSpeedKmPerHour = 35.0;
        return Math.max(1, (int) Math.round((distanceKm.doubleValue() / avgSpeedKmPerHour) * 60.0));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value) {
        return value == null ? "unknown" : value;
    }

    public record ScoredDriverCandidate(
            Users user,
            Driver driver,
            DriverAvailabilitySnapshot snapshot,
            BigDecimal score,
            BigDecimal distanceKm,
            Integer etaMinutes,
            List<String> reasons,
            List<String> penalties,
            List<String> rejectReasons
    ) {
        public boolean eligible() {
            return rejectReasons == null || rejectReasons.isEmpty();
        }
    }
}
