package com.example.project.domain.order.service;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverOrderRecommendationService {
    private static final Pattern TONNAGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final DateTimeFormatter ORDER_SCHEDULE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;

    public List<Order> getRecommendedOrders(Driver driver, Long currentUserId) {
        return orderRepository.findRecommendationCandidates()
                .stream()
                .filter(this::isAvailableBySchedule)
                .filter(order -> isRecommendedVehicleMatch(order, driver))
                .sorted(recommendedOrderComparator(driver, currentUserId))
                .collect(Collectors.toList());
    }

    public List<Order> searchOrders(Driver driver, Long currentUserId, Long targetNbhId, String address) {
        return orderRepository.findRecommendationCandidates()
                .stream()
                .filter(this::isAvailableBySchedule)
                .filter(order -> isRecommendedVehicleMatch(order, driver))
                .filter(order -> matchesSearchTarget(order, targetNbhId, address))
                .sorted(recommendedOrderComparator(driver, currentUserId))
                .collect(Collectors.toList());
    }

    private Comparator<Order> recommendedOrderComparator(Driver driver, Long currentUserId) {
        return Comparator
                .comparingInt((Order order) -> recommendationScore(order, driver, currentUserId))
                .reversed()
                .thenComparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int recommendationScore(Order order, Driver driver, Long currentUserId) {
        OrderSnapshot snapshot = order.getSnapshot();
        if (snapshot == null) {
            return Integer.MIN_VALUE;
        }

        int score = 0;

        if (isAppliedByDriver(order, currentUserId)) {
            score += 50;
        }

        if (driver.getNbhId() != null) {
            if (driver.getNbhId().equals(snapshot.getStartNbhId())) {
                score += 140;
            } else if (driver.getNbhId().equals(snapshot.getEndNbhId())) {
                score += 60;
            }
        }

        if (driver.getAddress() != null && snapshot.getStartAddr() != null) {
            String normalizedDriverAddress = normalizeAddress(driver.getAddress());
            String normalizedStartAddress = normalizeAddress(snapshot.getStartAddr());
            if (!normalizedDriverAddress.isBlank() && normalizedStartAddress.contains(normalizedDriverAddress)) {
                score += 90;
            } else {
                String[] addressTokens = driver.getAddress().split("\\s+");
                if (addressTokens.length > 0) {
                    String lastToken = normalizeAddress(addressTokens[addressTokens.length - 1]);
                    if (!lastToken.isBlank() && normalizedStartAddress.contains(lastToken)) {
                        score += 45;
                    }
                }
            }
        }

        Double distanceKm = calculateDistanceKm(
                driver.getLat(),
                driver.getLng(),
                snapshot.getStartLat() != null ? snapshot.getStartLat().doubleValue() : null,
                snapshot.getStartLng() != null ? snapshot.getStartLng().doubleValue() : null
        );
        if (distanceKm != null) {
            if (distanceKm <= 5) {
                score += 130;
            } else if (distanceKm <= 15) {
                score += 95;
            } else if (distanceKm <= 30) {
                score += 70;
            } else if (distanceKm <= 60) {
                score += 40;
            } else if (distanceKm <= 100) {
                score += 15;
            } else {
                score -= 10;
            }
        }

        BigDecimal driverTonnage = driver.getTonnage();
        BigDecimal requiredVehicleTonnage = parseRequestedVehicleTonnage(snapshot.getReqTonnage());
        if (driverTonnage != null && requiredVehicleTonnage != null) {
            BigDecimal spareCapacity = driverTonnage.subtract(requiredVehicleTonnage);
            if (spareCapacity.compareTo(BigDecimal.ZERO) >= 0) {
                score += spareCapacity.compareTo(new BigDecimal("1.5")) <= 0 ? 40 : 20;
            }
        }

        LocalDateTime startSchedule = parseSchedule(snapshot.getStartSchedule());
        if (startSchedule != null) {
            LocalDateTime nowTime = LocalDateTime.now();
            if (startSchedule.isBefore(nowTime.plusHours(3))) {
                score += 30;
            } else if (startSchedule.isBefore(nowTime.plusHours(12))) {
                score += 18;
            } else if (startSchedule.isBefore(nowTime.plusDays(1))) {
                score += 8;
            }
        }

        long fareAmount = safeLong(snapshot.getBasePrice())
                + safeLong(snapshot.getLaborFee())
                + safeLong(snapshot.getPackagingPrice())
                + safeLong(snapshot.getInsuranceFee());
        if (fareAmount >= 300_000L) {
            score += 25;
        } else if (fareAmount >= 150_000L) {
            score += 15;
        } else if (fareAmount >= 70_000L) {
            score += 8;
        }

        return score;
    }

    private boolean isRecommendedVehicleMatch(Order order, Driver driver) {
        if (order == null || driver == null || order.getSnapshot() == null) {
            return false;
        }

        OrderSnapshot snapshot = order.getSnapshot();
        String requiredCarType = normalizeCarType(snapshot.getReqCarType());
        String driverCarType = normalizeCarType(driver.getCarType());
        if (!requiredCarType.isBlank() && !driverCarType.isBlank() && !requiredCarType.equals(driverCarType)) {
            return false;
        }

        BigDecimal driverTonnage = driver.getTonnage();
        BigDecimal requiredVehicleTonnage = parseRequestedVehicleTonnage(snapshot.getReqTonnage());
        if (driverTonnage != null && requiredVehicleTonnage != null && driverTonnage.compareTo(requiredVehicleTonnage) < 0) {
            return false;
        }

        if (driverTonnage != null && snapshot.getTonnage() != null && driverTonnage.compareTo(snapshot.getTonnage()) < 0) {
            return false;
        }

        return true;
    }

    private boolean matchesSearchTarget(Order order, Long targetNbhId, String rawAddress) {
        if (order == null || order.getSnapshot() == null) {
            return false;
        }

        OrderSnapshot snapshot = order.getSnapshot();
        if (targetNbhId != null) {
            return targetNbhId.equals(snapshot.getStartNbhId()) || targetNbhId.equals(snapshot.getEndNbhId());
        }

        if (rawAddress == null || rawAddress.isBlank()) {
            return true;
        }

        String normalizedAddress = normalizeAddress(rawAddress);
        return normalizeAddress(snapshot.getStartAddr()).contains(normalizedAddress)
                || normalizeAddress(snapshot.getEndAddr()).contains(normalizedAddress);
    }

    private boolean isAvailableBySchedule(Order order) {
        if (order == null || order.getSnapshot() == null) {
            return false;
        }

        String startSchedule = order.getSnapshot().getStartSchedule();
        if (startSchedule == null || startSchedule.isBlank()) {
            return true;
        }

        LocalDateTime parsed = parseSchedule(startSchedule);
        if (parsed == null) {
            return true;
        }

        return parsed.isAfter(LocalDateTime.now());
    }

    private boolean isAppliedByDriver(Order order, Long currentUserId) {
        return currentUserId != null
                && order.getDriverList() != null
                && order.getDriverList().contains(currentUserId);
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

    private LocalDateTime parseSchedule(String startSchedule) {
        if (startSchedule == null || startSchedule.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(startSchedule.trim(), ORDER_SCHEDULE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double calculateDistanceKm(Double fromLat, Double fromLng, Double toLat, Double toLng) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(toLat - fromLat);
        double dLng = Math.toRadians(toLng - fromLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
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

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
