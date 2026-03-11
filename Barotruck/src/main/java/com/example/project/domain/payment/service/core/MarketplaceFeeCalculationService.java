package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.port.UserPort;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarketplaceFeeCalculationService {

    private final FeePolicyService feePolicyService;
    private final UserPort userPort;
    private final PromotionEligibilityService promotionEligibilityService;

    @Transactional(readOnly = true)
    public FeeBreakdownPreviewResponse calculate(CalculationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        FeePolicyResponse policy = feePolicyService.getCurrentPolicy();
        PromotionEligibilityService.PromotionContext shipperContext = resolveShipperContext(command);
        PromotionEligibilityService.PromotionContext driverContext = resolveDriverContext(command);

        return MarketplaceFeeMath.calculate(
                policy,
                normalizeBaseAmount(command.baseAmount()),
                shipperContext.userLevel(),
                driverContext.userLevel(),
                shipperContext.promoEligible(),
                driverContext.promoEligible(),
                command.includeTossFee()
        );
    }

    private BigDecimal normalizeBaseAmount(BigDecimal baseAmount) {
        if (baseAmount == null) {
            throw new IllegalArgumentException("baseAmount is required");
        }
        if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("baseAmount must be >= 0");
        }
        return baseAmount;
    }

    private PromotionEligibilityService.PromotionContext resolveShipperContext(CalculationCommand command) {
        if (command.orderId() != null && command.shipperPromoEligible() == null && command.shipperUserLevel() == null) {
            return promotionEligibilityService.getShipperFirstPaymentContext(command.orderId());
        }
        Long userId = command.shipperUserId();
        Long userLevel = resolveUserLevel(command.shipperUserLevel(), userId);
        boolean promoEligible = command.shipperPromoEligible() != null && command.shipperPromoEligible();
        return new PromotionEligibilityService.PromotionContext(userId, userLevel, promoEligible);
    }

    private PromotionEligibilityService.PromotionContext resolveDriverContext(CalculationCommand command) {
        if (command.orderId() != null && command.driverPromoEligible() == null && command.driverUserLevel() == null) {
            try {
                return promotionEligibilityService.getDriverFirstTransportContext(command.orderId());
            } catch (IllegalStateException e) {
                return new PromotionEligibilityService.PromotionContext(
                        command.driverUserId(),
                        resolveUserLevel(command.driverUserLevel(), command.driverUserId()),
                        false
                );
            }
        }
        Long userId = command.driverUserId();
        Long userLevel = resolveUserLevel(command.driverUserLevel(), userId);
        boolean promoEligible = command.driverPromoEligible() != null && command.driverPromoEligible();
        return new PromotionEligibilityService.PromotionContext(userId, userLevel, promoEligible);
    }

    private Long resolveUserLevel(Long explicitLevel, Long userId) {
        if (explicitLevel != null) {
            return explicitLevel;
        }
        if (userId == null) {
            return 0L;
        }
        return userPort.getRequiredUser(userId).userLevel();
    }

    @Builder
    public record CalculationCommand(
            Long orderId,
            BigDecimal baseAmount,
            Long shipperUserId,
            Long driverUserId,
            Long shipperUserLevel,
            Long driverUserLevel,
            Boolean shipperPromoEligible,
            Boolean driverPromoEligible,
            boolean includeTossFee
    ) {
    }
}
