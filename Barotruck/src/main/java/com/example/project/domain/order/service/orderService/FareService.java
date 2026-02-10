package com.example.project.domain.order.service.orderService;

import com.example.project.domain.order.domain.FarePolicy;
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.repository.FareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FareService {

    private final FareRepository fareRepository;

    // 학원 레벨 기본값: 야간 22:00~06:00, 거리 km는 올림(ceiling)
    private static final int NIGHT_START_HOUR = 22;
    private static final int NIGHT_END_HOUR = 6;

    public long estimateFare(FareRequest req) {
        validate(req);

        LocalDateTime pickupAt = req.getPickupAt();
        long distanceMeters = req.getDistanceMeters();

        FarePolicy.DayType dayType = resolveDayType(pickupAt, req.getIsHoliday());
        FarePolicy.TimeType timeType = resolveTimeType(pickupAt);

        FarePolicy policy = fareRepository
                .findTop1ByDayTypeAndTimeType(dayType, timeType)
                .orElseThrow(() -> new IllegalStateException("해당 요금표가 없습니다. dayType=" + dayType + ", timeType=" + timeType));

        int distanceKm = metersToChargedKm(distanceMeters); // 과금 km (올림)
        int extraKm = Math.max(0, distanceKm - policy.getBaseDistanceKm());

        long fare = policy.getBaseFare() + (policy.getPerKmFare() * extraKm);

        if (policy.getMinimumFare() != null) {
            fare = Math.max(fare, policy.getMinimumFare());
        }

        return fare;
    }

    private void validate(FareRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");
        if (req.getPickupAt() == null) throw new IllegalArgumentException("pickupAt is required");
        if (req.getDistanceMeters() == null || req.getDistanceMeters() < 0) {
            throw new IllegalArgumentException("distanceMeters is invalid");
        }
        if (req.getIsHoliday() == null) {
            req.setIsHoliday(false);
        }
    }

    private FarePolicy.DayType resolveDayType(LocalDateTime pickupAt, boolean isHoliday) {
        if (isHoliday) return FarePolicy.DayType.HOLIDAY;

        DayOfWeek dow = pickupAt.getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        return weekend ? FarePolicy.DayType.WEEKEND : FarePolicy.DayType.WEEKDAY;
    }

    private FarePolicy.TimeType resolveTimeType(LocalDateTime pickupAt) {
        int hour = pickupAt.getHour();

        boolean isNight = (hour >= NIGHT_START_HOUR) || (hour < NIGHT_END_HOUR);
        return isNight ? FarePolicy.TimeType.NIGHT : FarePolicy.TimeType.DAY;
    }

    // meters -> km 과금단위(올림): 0m=0km, 1m=1km, 1001m=2km
    private int metersToChargedKm(long meters) {
        if (meters == 0) return 0;
        return (int) ((meters + 999) / 1000);
    }
}
