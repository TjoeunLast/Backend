package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeePolicyConfig;
import com.example.project.domain.payment.dto.paymentRequest.UpdateLevelFeeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateFeePolicyRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.LevelFeePolicyResponse;
import com.example.project.domain.payment.repository.FeePolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeePolicyService {

    private static final BigDecimal DEFAULT_LEVEL0_RATE = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_LEVEL1_RATE = new BigDecimal("0.04");
    private static final BigDecimal DEFAULT_LEVEL2_RATE = new BigDecimal("0.03");
    private static final BigDecimal DEFAULT_LEVEL3_PLUS_RATE = new BigDecimal("0.03");
    private static final BigDecimal DEFAULT_FIRST_PAYMENT_PROMO_RATE = new BigDecimal("0.03");
    private static final BigDecimal DEFAULT_MIN_FEE = new BigDecimal("2000.00");

    private final FeePolicyConfigRepository feePolicyConfigRepository;

    public record FeeResult(
            BigDecimal feeRate,
            BigDecimal feeAmount,
            BigDecimal chargedAmount,
            BigDecimal driverAmount,
            boolean promoApplied
    ) {
    }

    @Transactional(readOnly = true)
    public FeePolicyResponse getCurrentPolicy() {
        ResolvedPolicy policy = resolvePolicy();
        return toPolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public LevelFeePolicyResponse getCurrentPolicyByLevel(Long level) {
        ResolvedPolicy policy = resolvePolicy();
        return toLevelResponse(level, policy);
    }

    @Transactional
    public FeePolicyResponse updatePolicy(UpdateFeePolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        FeePolicyConfig saved = feePolicyConfigRepository.save(
                FeePolicyConfig.builder()
                        .level0Rate(normalizeRate(request.getLevel0Rate(), "level0Rate"))
                        .level1Rate(normalizeRate(request.getLevel1Rate(), "level1Rate"))
                        .level2Rate(normalizeRate(request.getLevel2Rate(), "level2Rate"))
                        .level3PlusRate(normalizeRate(request.getLevel3PlusRate(), "level3PlusRate"))
                        .firstPaymentPromoRate(normalizeRate(request.getFirstPaymentPromoRate(), "firstPaymentPromoRate"))
                        .minFee(normalizeMinFee(request.getMinFee()))
                        .build()
        );

        return toPolicyResponse(saved);
    }

    @Transactional
    public LevelFeePolicyResponse updateLevelPolicy(UpdateLevelFeeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        long appliedLevel = normalizeLevelBucket(request.getLevel());
        ResolvedPolicy current = resolvePolicy();
        BigDecimal normalizedRate = normalizeRate(request.getRate(), "rate");

        FeePolicyConfig saved = feePolicyConfigRepository.save(
                FeePolicyConfig.builder()
                        .level0Rate(appliedLevel == 0 ? normalizedRate : current.level0Rate())
                        .level1Rate(appliedLevel == 1 ? normalizedRate : current.level1Rate())
                        .level2Rate(appliedLevel == 2 ? normalizedRate : current.level2Rate())
                        .level3PlusRate(appliedLevel >= 3 ? normalizedRate : current.level3PlusRate())
                        .firstPaymentPromoRate(current.firstPaymentPromoRate())
                        .minFee(current.minFee())
                        .build()
        );

        return toLevelResponse(request.getLevel(), toResolvedPolicy(saved));
    }

    public FeeResult calculate(BigDecimal amount, Long userLevel, boolean firstPaymentPromoEligible) {
        ResolvedPolicy policy = resolvePolicy();
        BigDecimal rate = mapRate(userLevel, policy);

        boolean promoApplied = false;
        if (firstPaymentPromoEligible) {
            rate = policy.firstPaymentPromoRate();
            promoApplied = true;
        }

        BigDecimal fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(policy.minFee()) < 0) {
            fee = policy.minFee();
        }

        BigDecimal charged = amount.add(fee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverAmount = amount.setScale(2, RoundingMode.HALF_UP);
        return new FeeResult(rate, fee, charged, driverAmount, promoApplied);
    }

    private BigDecimal mapRate(Long userLevel, ResolvedPolicy policy) {
        if (userLevel == null) {
            return policy.level0Rate();
        }

        return switch (userLevel.intValue()) {
            case 0 -> policy.level0Rate();
            case 1 -> policy.level1Rate();
            case 2 -> policy.level2Rate();
            default -> policy.level3PlusRate();
        };
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

    private ResolvedPolicy resolvePolicy() {
        FeePolicyConfig config = feePolicyConfigRepository.findTopByOrderByPolicyIdDesc().orElse(null);
        if (config == null) {
            return new ResolvedPolicy(
                    DEFAULT_LEVEL0_RATE,
                    DEFAULT_LEVEL1_RATE,
                    DEFAULT_LEVEL2_RATE,
                    DEFAULT_LEVEL3_PLUS_RATE,
                    DEFAULT_FIRST_PAYMENT_PROMO_RATE,
                    DEFAULT_MIN_FEE,
                    null
            );
        }

        return new ResolvedPolicy(
                nullSafeRate(config.getLevel0Rate(), DEFAULT_LEVEL0_RATE),
                nullSafeRate(config.getLevel1Rate(), DEFAULT_LEVEL1_RATE),
                nullSafeRate(config.getLevel2Rate(), DEFAULT_LEVEL2_RATE),
                nullSafeRate(config.getLevel3PlusRate(), DEFAULT_LEVEL3_PLUS_RATE),
                nullSafeRate(config.getFirstPaymentPromoRate(), DEFAULT_FIRST_PAYMENT_PROMO_RATE),
                nullSafeMinFee(config.getMinFee()),
                config.getCreatedAt()
        );
    }

    private ResolvedPolicy toResolvedPolicy(FeePolicyConfig config) {
        return new ResolvedPolicy(
                nullSafeRate(config.getLevel0Rate(), DEFAULT_LEVEL0_RATE),
                nullSafeRate(config.getLevel1Rate(), DEFAULT_LEVEL1_RATE),
                nullSafeRate(config.getLevel2Rate(), DEFAULT_LEVEL2_RATE),
                nullSafeRate(config.getLevel3PlusRate(), DEFAULT_LEVEL3_PLUS_RATE),
                nullSafeRate(config.getFirstPaymentPromoRate(), DEFAULT_FIRST_PAYMENT_PROMO_RATE),
                nullSafeMinFee(config.getMinFee()),
                config.getCreatedAt()
        );
    }

    private FeePolicyResponse toPolicyResponse(ResolvedPolicy policy) {
        return FeePolicyResponse.builder()
                .level0Rate(policy.level0Rate())
                .level1Rate(policy.level1Rate())
                .level2Rate(policy.level2Rate())
                .level3PlusRate(policy.level3PlusRate())
                .firstPaymentPromoRate(policy.firstPaymentPromoRate())
                .minFee(policy.minFee())
                .updatedAt(policy.updatedAt())
                .build();
    }

    private FeePolicyResponse toPolicyResponse(FeePolicyConfig config) {
        return FeePolicyResponse.builder()
                .level0Rate(config.getLevel0Rate())
                .level1Rate(config.getLevel1Rate())
                .level2Rate(config.getLevel2Rate())
                .level3PlusRate(config.getLevel3PlusRate())
                .firstPaymentPromoRate(config.getFirstPaymentPromoRate())
                .minFee(config.getMinFee())
                .updatedAt(config.getCreatedAt())
                .build();
    }

    private LevelFeePolicyResponse toLevelResponse(Long requestedLevel, ResolvedPolicy policy) {
        long appliedLevel = normalizeLevelBucket(requestedLevel);
        BigDecimal rate = switch ((int) appliedLevel) {
            case 0 -> policy.level0Rate();
            case 1 -> policy.level1Rate();
            case 2 -> policy.level2Rate();
            default -> policy.level3PlusRate();
        };

        return LevelFeePolicyResponse.builder()
                .requestedLevel(requestedLevel)
                .appliedLevel(appliedLevel)
                .rate(rate)
                .firstPaymentPromoRate(policy.firstPaymentPromoRate())
                .minFee(policy.minFee())
                .updatedAt(policy.updatedAt())
                .build();
    }

    private BigDecimal nullSafeRate(BigDecimal value, BigDecimal defaultValue) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return defaultValue;
        }
        return value;
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
            BigDecimal level0Rate,
            BigDecimal level1Rate,
            BigDecimal level2Rate,
            BigDecimal level3PlusRate,
            BigDecimal firstPaymentPromoRate,
            BigDecimal minFee,
            LocalDateTime updatedAt
    ) {
    }
}
