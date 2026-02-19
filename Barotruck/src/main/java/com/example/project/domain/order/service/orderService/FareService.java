package com.example.project.domain.order.service.orderService;

import com.example.project.domain.order.domain.FarePolicy;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.dto.orderRequest.FareRequest;
import com.example.project.domain.order.repository.FareRepository;
import com.example.project.domain.order.repository.OrderRepository;
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

    // 추가: 주문 저장을 위해 필요
    private final OrderRepository orderRepository;

    // 학원 레벨 기본값: 야간 22:00~06:00, 거리 km는 올림(ceiling)
    private static final int NIGHT_START_HOUR = 22;
    private static final int NIGHT_END_HOUR = 6;

    // 기존: 견적만 계산
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

    // 추가: 견적 계산 + ORDERS 테이블에 저장(확정)
    @Transactional
    public long estimateAndSaveFare(Long orderId, FareRequest req) {
        validate(req);

        // 1) 기본 운임 계산
        long baseFare = estimateFare(req);

        // 2) 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        // 3) 추가요금은 OrderSnapshot에 있음
        OrderSnapshot snap = order.getSnapshot();
        if (snap == null) {
            throw new IllegalStateException("order snapshot not found: " + orderId);
        }

        long labor = snap.getLaborFee() == null ? 0L : snap.getLaborFee();
        long packaging = snap.getPackagingPrice() == null ? 0L : snap.getPackagingPrice();
        long insurance = snap.getInsuranceFee() == null ? 0L : snap.getInsuranceFee();

        long totalPrice = baseFare + labor + packaging + insurance;

        // 4) DB 업데이트 (native query)
        int updated = orderRepository.updateFareSnapshot(
                orderId,
                baseFare,
                totalPrice,
                req.getDistanceMeters()
        );

        if (updated == 0) {
            throw new IllegalStateException("fare update failed: " + orderId);
        }

        return totalPrice;
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

    private long nullSafe(Long v) {
        return v == null ? 0L : v;
    }
}
