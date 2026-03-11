package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeePolicyConfig;
import com.example.project.domain.payment.dto.paymentRequest.FeePolicySideRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateFeePolicyRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateLevelFeeRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicySideResponse;
import com.example.project.domain.payment.dto.paymentResponse.LevelFeePolicyResponse;
import com.example.project.domain.payment.repository.FeePolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FeePolicyService {

    private static final SideRates DEFAULT_SHIPPER_SIDE_RATES = new SideRates(
            new BigDecimal("0.0250"),
            new BigDecimal("0.0200"),
            new BigDecimal("0.0180"),
            new BigDecimal("0.0150")
    );
    private static final SideRates DEFAULT_DRIVER_SIDE_RATES = new SideRates(
            new BigDecimal("0.0250"),
            new BigDecimal("0.0200"),
            new BigDecimal("0.0180"),
            new BigDecimal("0.0150")
    );
    private static final BigDecimal DEFAULT_SHIPPER_FIRST_PAYMENT_PROMO_RATE = new BigDecimal("0.0150");
    private static final BigDecimal DEFAULT_DRIVER_FIRST_TRANSPORT_PROMO_RATE = new BigDecimal("0.0150");
    private static final BigDecimal DEFAULT_TOSS_RATE = new BigDecimal("0.1000");
    private static final BigDecimal DEFAULT_MIN_FEE = new BigDecimal("2000.00");

    private final FeePolicyConfigRepository feePolicyConfigRepository;

    public record FeeResult(
            BigDecimal feeRate,
            BigDecimal feeAmount,
            BigDecimal chargedAmount,
            BigDecimal driverAmount,
            boolean promoApplied,
            Long policyId,
            LocalDateTime policyAppliedAt
    ) {
    }

    @Transactional(readOnly = true)
    public FeePolicyResponse getCurrentPolicy() {
        return toPolicyResponse(resolvePolicy());
    }

    @Transactional(readOnly = true)
    public LevelFeePolicyResponse getCurrentPolicyByLevel(Long level) {
        return toLevelResponse(level, PolicySide.SHIPPER, resolvePolicy());
    }

    @Transactional
    public FeePolicyResponse updatePolicy(UpdateFeePolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!request.hasAnyUpdates()) {
            throw new IllegalArgumentException("at least one fee policy field is required");
        }

        ResolvedPolicy current = resolvePolicy();
        ResolvedPolicy updated = new ResolvedPolicy(
                mergeShipperSide(current.shipperSide(), request),
                mergeDriverSide(current.driverSide(), request.getDriverSide()),
                mergeRate(
                        request.getShipperFirstPaymentPromoRate(),
                        request.getFirstPaymentPromoRate(),
                        current.shipperFirstPaymentPromoRate(),
                        "shipperFirstPaymentPromoRate"
                ),
                mergeRate(
                        request.getDriverFirstTransportPromoRate(),
                        null,
                        current.driverFirstTransportPromoRate(),
                        "driverFirstTransportPromoRate"
                ),
                mergeRate(request.getTossRate(), null, current.tossRate(), "tossRate"),
                mergeMinFee(request.getMinFee(), current.minFee()),
                current.policyId(),
                current.updatedAt()
        );

        return toPolicyResponse(toResolvedPolicy(savePolicy(updated)));
    }

    @Transactional
    public LevelFeePolicyResponse updateLevelPolicy(UpdateLevelFeeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        long appliedLevel = normalizeLevelBucket(request.getLevel());
        PolicySide side = normalizeSide(request.getSide());
        ResolvedPolicy current = resolvePolicy();
        BigDecimal normalizedRate = normalizeRate(request.getRate(), "rate");

        SideRates shipperSide = current.shipperSide();
        SideRates driverSide = current.driverSide();

        if (side == PolicySide.SHIPPER) {
            shipperSide = shipperSide.withRate(appliedLevel, normalizedRate);
        } else {
            driverSide = driverSide.withRate(appliedLevel, normalizedRate);
        }

        ResolvedPolicy updated = new ResolvedPolicy(
                shipperSide,
                driverSide,
                current.shipperFirstPaymentPromoRate(),
                current.driverFirstTransportPromoRate(),
                current.tossRate(),
                current.minFee(),
                current.policyId(),
                current.updatedAt()
        );

        return toLevelResponse(request.getLevel(), side, toResolvedPolicy(savePolicy(updated)));
    }

    public FeeResult calculate(BigDecimal amount, Long userLevel, boolean firstPaymentPromoEligible) {
        ResolvedPolicy policy = resolvePolicy();
        var breakdown = MarketplaceFeeMath.calculate(
                toPolicyResponse(policy),
                amount,
                userLevel,
                null,
                firstPaymentPromoEligible,
                false,
                false
        );

        return new FeeResult(
                breakdown.shipperFeeRate(),
                breakdown.shipperFeeAmount(),
                breakdown.shipperChargeAmount(),
                breakdown.baseAmount() == null
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : breakdown.baseAmount().setScale(2, RoundingMode.HALF_UP),
                breakdown.shipperPromoApplied(),
                policy.policyId(),
                policy.updatedAt()
        );
    }

    private BigDecimal mapRate(Long userLevel, SideRates sideRates) {
        if (userLevel == null) {
            return sideRates.level0Rate();
        }
        return sideRates.rateFor(normalizeLevelBucket(userLevel));
    }

    private long normalizeLevelBucket(Long level) {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level must be >= 0");
        }
        return Math.min(level, 3L);
    }

    private PolicySide normalizeSide(String rawSide) {
        if (rawSide == null || rawSide.isBlank()) {
            return PolicySide.SHIPPER;
        }

        return switch (rawSide.trim().toUpperCase(Locale.ROOT)) {
            case "SHIPPER" -> PolicySide.SHIPPER;
            case "DRIVER" -> PolicySide.DRIVER;
            default -> throw new IllegalArgumentException("side must be shipper or driver");
        };
    }

    private ResolvedPolicy resolvePolicy() {
        FeePolicyConfig config = feePolicyConfigRepository.findTopByOrderByPolicyIdDesc().orElse(null);
        if (config == null) {
            return defaultPolicy();
        }

        return new ResolvedPolicy(
                resolveShipperSide(config),
                resolveDriverSide(config),
                nullSafeRate(
                        firstNonNull(config.getShipperFirstPaymentPromoRate(), config.getFirstPaymentPromoRate()),
                        DEFAULT_SHIPPER_FIRST_PAYMENT_PROMO_RATE
                ),
                nullSafeRate(config.getDriverFirstTransportPromoRate(), DEFAULT_DRIVER_FIRST_TRANSPORT_PROMO_RATE),
                nullSafeRate(config.getTossRate(), DEFAULT_TOSS_RATE),
                nullSafeMinFee(config.getMinFee()),
                config.getPolicyId(),
                config.getCreatedAt()
        );
    }

    private ResolvedPolicy toResolvedPolicy(FeePolicyConfig config) {
        return new ResolvedPolicy(
                resolveShipperSide(config),
                resolveDriverSide(config),
                nullSafeRate(
                        firstNonNull(config.getShipperFirstPaymentPromoRate(), config.getFirstPaymentPromoRate()),
                        DEFAULT_SHIPPER_FIRST_PAYMENT_PROMO_RATE
                ),
                nullSafeRate(config.getDriverFirstTransportPromoRate(), DEFAULT_DRIVER_FIRST_TRANSPORT_PROMO_RATE),
                nullSafeRate(config.getTossRate(), DEFAULT_TOSS_RATE),
                nullSafeMinFee(config.getMinFee()),
                config.getPolicyId(),
                config.getCreatedAt()
        );
    }

    private ResolvedPolicy defaultPolicy() {
        return new ResolvedPolicy(
                DEFAULT_SHIPPER_SIDE_RATES,
                DEFAULT_DRIVER_SIDE_RATES,
                DEFAULT_SHIPPER_FIRST_PAYMENT_PROMO_RATE,
                DEFAULT_DRIVER_FIRST_TRANSPORT_PROMO_RATE,
                DEFAULT_TOSS_RATE,
                DEFAULT_MIN_FEE,
                0L,
                null
        );
    }

    private SideRates resolveShipperSide(FeePolicyConfig config) {
        return new SideRates(
                nullSafeRate(firstNonNull(config.getShipperLevel0Rate(), config.getLevel0Rate()), DEFAULT_SHIPPER_SIDE_RATES.level0Rate()),
                nullSafeRate(firstNonNull(config.getShipperLevel1Rate(), config.getLevel1Rate()), DEFAULT_SHIPPER_SIDE_RATES.level1Rate()),
                nullSafeRate(firstNonNull(config.getShipperLevel2Rate(), config.getLevel2Rate()), DEFAULT_SHIPPER_SIDE_RATES.level2Rate()),
                nullSafeRate(firstNonNull(config.getShipperLevel3PlusRate(), config.getLevel3PlusRate()), DEFAULT_SHIPPER_SIDE_RATES.level3PlusRate())
        );
    }

    private SideRates resolveDriverSide(FeePolicyConfig config) {
        return new SideRates(
                nullSafeRate(config.getDriverLevel0Rate(), DEFAULT_DRIVER_SIDE_RATES.level0Rate()),
                nullSafeRate(config.getDriverLevel1Rate(), DEFAULT_DRIVER_SIDE_RATES.level1Rate()),
                nullSafeRate(config.getDriverLevel2Rate(), DEFAULT_DRIVER_SIDE_RATES.level2Rate()),
                nullSafeRate(config.getDriverLevel3PlusRate(), DEFAULT_DRIVER_SIDE_RATES.level3PlusRate())
        );
    }

    private SideRates mergeShipperSide(SideRates current, UpdateFeePolicyRequest request) {
        FeePolicySideRequest shipperSide = request.getShipperSide();
        return new SideRates(
                mergeRate(shipperSide != null ? shipperSide.getLevel0Rate() : null, request.getLevel0Rate(), current.level0Rate(), "shipperSide.level0Rate"),
                mergeRate(shipperSide != null ? shipperSide.getLevel1Rate() : null, request.getLevel1Rate(), current.level1Rate(), "shipperSide.level1Rate"),
                mergeRate(shipperSide != null ? shipperSide.getLevel2Rate() : null, request.getLevel2Rate(), current.level2Rate(), "shipperSide.level2Rate"),
                mergeRate(shipperSide != null ? shipperSide.getLevel3PlusRate() : null, request.getLevel3PlusRate(), current.level3PlusRate(), "shipperSide.level3PlusRate")
        );
    }

    private SideRates mergeDriverSide(SideRates current, FeePolicySideRequest request) {
        return new SideRates(
                mergeRate(request != null ? request.getLevel0Rate() : null, null, current.level0Rate(), "driverSide.level0Rate"),
                mergeRate(request != null ? request.getLevel1Rate() : null, null, current.level1Rate(), "driverSide.level1Rate"),
                mergeRate(request != null ? request.getLevel2Rate() : null, null, current.level2Rate(), "driverSide.level2Rate"),
                mergeRate(request != null ? request.getLevel3PlusRate() : null, null, current.level3PlusRate(), "driverSide.level3PlusRate")
        );
    }

    private BigDecimal mergeRate(BigDecimal primaryValue, BigDecimal fallbackValue, BigDecimal currentValue, String fieldName) {
        BigDecimal candidate = firstNonNull(primaryValue, fallbackValue);
        if (candidate == null) {
            return currentValue;
        }
        return normalizeRate(candidate, fieldName);
    }

    private BigDecimal mergeMinFee(BigDecimal requestedValue, BigDecimal currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        return normalizeMinFee(requestedValue);
    }

    private FeePolicyConfig savePolicy(ResolvedPolicy policy) {
        return feePolicyConfigRepository.save(
                FeePolicyConfig.builder()
                        .level0Rate(policy.shipperSide().level0Rate())
                        .level1Rate(policy.shipperSide().level1Rate())
                        .level2Rate(policy.shipperSide().level2Rate())
                        .level3PlusRate(policy.shipperSide().level3PlusRate())
                        .firstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                        .shipperLevel0Rate(policy.shipperSide().level0Rate())
                        .shipperLevel1Rate(policy.shipperSide().level1Rate())
                        .shipperLevel2Rate(policy.shipperSide().level2Rate())
                        .shipperLevel3PlusRate(policy.shipperSide().level3PlusRate())
                        .driverLevel0Rate(policy.driverSide().level0Rate())
                        .driverLevel1Rate(policy.driverSide().level1Rate())
                        .driverLevel2Rate(policy.driverSide().level2Rate())
                        .driverLevel3PlusRate(policy.driverSide().level3PlusRate())
                        .shipperFirstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                        .driverFirstTransportPromoRate(policy.driverFirstTransportPromoRate())
                        .tossRate(policy.tossRate())
                        .minFee(policy.minFee())
                        .build()
        );
    }

    private FeePolicyResponse toPolicyResponse(ResolvedPolicy policy) {
        return FeePolicyResponse.builder()
                .policyConfigId(policy.policyId())
                .level0Rate(policy.shipperSide().level0Rate())
                .level1Rate(policy.shipperSide().level1Rate())
                .level2Rate(policy.shipperSide().level2Rate())
                .level3PlusRate(policy.shipperSide().level3PlusRate())
                .firstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                .shipperSide(toSideResponse(policy.shipperSide()))
                .driverSide(toSideResponse(policy.driverSide()))
                .shipperFirstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                .driverFirstTransportPromoRate(policy.driverFirstTransportPromoRate())
                .tossRate(policy.tossRate())
                .minFee(policy.minFee())
                .updatedAt(policy.updatedAt())
                .build();
    }

    private FeePolicySideResponse toSideResponse(SideRates sideRates) {
        return FeePolicySideResponse.builder()
                .level0Rate(sideRates.level0Rate())
                .level1Rate(sideRates.level1Rate())
                .level2Rate(sideRates.level2Rate())
                .level3PlusRate(sideRates.level3PlusRate())
                .build();
    }

    private LevelFeePolicyResponse toLevelResponse(Long requestedLevel, PolicySide side, ResolvedPolicy policy) {
        long appliedLevel = normalizeLevelBucket(requestedLevel);
        BigDecimal shipperRate = policy.shipperSide().rateFor(appliedLevel);
        BigDecimal driverRate = policy.driverSide().rateFor(appliedLevel);

        return LevelFeePolicyResponse.builder()
                .requestedLevel(requestedLevel)
                .appliedLevel(appliedLevel)
                .side(side.apiName())
                .rate(shipperRate)
                .firstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                .shipperRate(shipperRate)
                .driverRate(driverRate)
                .shipperFirstPaymentPromoRate(policy.shipperFirstPaymentPromoRate())
                .driverFirstTransportPromoRate(policy.driverFirstTransportPromoRate())
                .tossRate(policy.tossRate())
                .minFee(policy.minFee())
                .updatedAt(policy.updatedAt())
                .build();
    }

    private BigDecimal firstNonNull(BigDecimal primary, BigDecimal fallback) {
        return primary != null ? primary : fallback;
    }

    private BigDecimal nullSafeRate(BigDecimal value, BigDecimal defaultValue) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return defaultValue;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafeMinFee(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return DEFAULT_MIN_FEE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMinFee(BigDecimal minFee) {
        if (minFee == null) {
            throw new IllegalArgumentException("minFee is required");
        }
        if (minFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minFee must be >= 0");
        }
        return minFee.setScale(2, RoundingMode.HALF_UP);
    }

    private record ResolvedPolicy(
            SideRates shipperSide,
            SideRates driverSide,
            BigDecimal shipperFirstPaymentPromoRate,
            BigDecimal driverFirstTransportPromoRate,
            BigDecimal tossRate,
            BigDecimal minFee,
            Long policyId,
            LocalDateTime updatedAt
    ) {
    }

    private record SideRates(
            BigDecimal level0Rate,
            BigDecimal level1Rate,
            BigDecimal level2Rate,
            BigDecimal level3PlusRate
    ) {
        private BigDecimal rateFor(long level) {
            return switch ((int) level) {
                case 0 -> level0Rate;
                case 1 -> level1Rate;
                case 2 -> level2Rate;
                default -> level3PlusRate;
            };
        }

        private SideRates withRate(long level, BigDecimal updatedRate) {
            return switch ((int) level) {
                case 0 -> new SideRates(updatedRate, level1Rate, level2Rate, level3PlusRate);
                case 1 -> new SideRates(level0Rate, updatedRate, level2Rate, level3PlusRate);
                case 2 -> new SideRates(level0Rate, level1Rate, updatedRate, level3PlusRate);
                default -> new SideRates(level0Rate, level1Rate, level2Rate, updatedRate);
            };
        }
    }

    private enum PolicySide {
        SHIPPER,
        DRIVER;

        private String apiName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
