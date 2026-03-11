package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentRequest.FeePreviewRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentFeePreviewService {

    private static final String PREVIEW_MODE_ORDER_CREATE = "ORDER_CREATE";
    private static final String PREVIEW_MODE_PAYMENT_ENTRY = "PAYMENT_ENTRY";

    private final OrderPort orderPort;
    private final UserPort userPort;
    private final FeePolicyService feePolicyService;
    private final PromotionEligibilityService promotionEligibilityService;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;

    @Transactional(readOnly = true)
    public FeeBreakdownPreviewResponse preview(Users currentUser, FeePreviewRequest request) {
        requireAuthenticated(currentUser);
        validatePreviewRole(currentUser);

        PreviewContext context = resolveContext(currentUser, request);
        PreviewPayment payment = resolvePreviewPayment(request, context);
        PreviewParticipant shipper = resolveShipperParticipant(context);
        PreviewParticipant driver = resolveDriverParticipant(context);
        FeePolicyResponse policy = feePolicyService.getCurrentPolicy();

        FeeBreakdownPreviewResponse calculated = MarketplaceFeeMath.calculate(
                policy,
                context.baseAmount(),
                shipper.appliedLevel(),
                driver == null ? null : driver.appliedLevel(),
                shipper.promoEligible(),
                driver != null && driver.promoEligible(),
                payment.provider() == PaymentProvider.TOSS
        );

        return finalizeResponse(context.previewMode(), payment.provider(), shipper, driver, calculated);
    }

    private PreviewContext resolveContext(Users currentUser, FeePreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getOrderId() != null && request.getBaseAmount() != null) {
            throw new IllegalArgumentException("orderId and baseAmount cannot be used together");
        }

        if (request.getOrderId() != null) {
            OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(request.getOrderId());
            validateOrderAccess(currentUser, snap);
            validatePaymentPreviewOrderStatus(snap.status());

            return new PreviewContext(
                    snap.orderId(),
                    PREVIEW_MODE_PAYMENT_ENTRY,
                    requiredUserId(snap.shipperUserId(), "shipperUserId"),
                    snap.driverUserId(),
                    normalizeAmount(snap.amount()),
                    resolveOrderPaymentMethod(snap.payMethod())
            );
        }

        if (request.getBaseAmount() == null) {
            throw new IllegalArgumentException("orderId or baseAmount is required");
        }
        validateNonNegative(request.getBaseAmount(), "baseAmount");

        Long shipperUserId;
        if (currentUser.getRole() == Role.SHIPPER) {
            shipperUserId = currentUser.getUserId();
            if (request.getShipperUserId() != null && !Objects.equals(request.getShipperUserId(), shipperUserId)) {
                throw new IllegalStateException("shipper can preview only own fee");
            }
        } else {
            shipperUserId = requiredUserId(request.getShipperUserId(), "shipperUserId");
        }

        return new PreviewContext(
                null,
                PREVIEW_MODE_ORDER_CREATE,
                shipperUserId,
                request.getDriverUserId(),
                normalizeAmount(request.getBaseAmount()),
                request.getPaymentMethod()
        );
    }

    private PreviewParticipant resolveShipperParticipant(PreviewContext context) {
        if (context.orderId() != null) {
            PromotionEligibilityService.PromotionContext shipperContext =
                    promotionEligibilityService.getShipperFirstPaymentContext(context.orderId());
            return PreviewParticipant.from(shipperContext);
        }

        UserPort.UserInfo shipper = userPort.getRequiredUser(context.shipperUserId());
        boolean promoEligible = transportPaymentRepository.countByShipperUserIdAndStatusIn(
                context.shipperUserId(),
                paidOrConfirmedStatuses()
        ) == 0;
        return new PreviewParticipant(shipper.userId(), shipper.userLevel(), normalizeLevelBucket(shipper.userLevel()), promoEligible);
    }

    private PreviewParticipant resolveDriverParticipant(PreviewContext context) {
        if (context.driverUserId() == null) {
            return null;
        }

        if (context.orderId() != null) {
            try {
                PromotionEligibilityService.PromotionContext driverContext =
                        promotionEligibilityService.getDriverFirstTransportContext(context.orderId());
                return PreviewParticipant.from(driverContext);
            } catch (IllegalStateException ignored) {
                // Fallback to direct user lookup when payout-side promotion context is not yet resolvable.
            }
        }

        UserPort.UserInfo driver = userPort.getRequiredUser(context.driverUserId());
        boolean promoEligible = driverPayoutItemRepository.countByDriverUserId(context.driverUserId()) == 0;
        return new PreviewParticipant(driver.userId(), driver.userLevel(), normalizeLevelBucket(driver.userLevel()), promoEligible);
    }

    private FeeBreakdownPreviewResponse finalizeResponse(
            String previewMode,
            PaymentProvider paymentProvider,
            PreviewParticipant shipper,
            PreviewParticipant driver,
            FeeBreakdownPreviewResponse calculated
    ) {
        if (driver != null) {
            return FeeBreakdownPreviewResponse.builder()
                    .previewMode(previewMode)
                    .paymentProvider(paymentProvider)
                    .baseAmount(calculated.baseAmount())
                    .postTossBaseAmount(calculated.postTossBaseAmount())
                    .shipperAppliedLevel(calculated.shipperAppliedLevel())
                    .driverAppliedLevel(calculated.driverAppliedLevel())
                    .shipperFeeRate(calculated.shipperFeeRate())
                    .driverFeeRate(calculated.driverFeeRate())
                    .shipperFeeAmount(calculated.shipperFeeAmount())
                    .driverFeeAmount(calculated.driverFeeAmount())
                    .shipperPromoEligible(shipper.promoEligible())
                    .driverPromoEligible(driver.promoEligible())
                    .shipperPromoApplied(calculated.shipperPromoApplied())
                    .driverPromoApplied(calculated.driverPromoApplied())
                    .shipperMinFeeApplied(calculated.shipperMinFeeApplied())
                    .driverMinFeeApplied(calculated.driverMinFeeApplied())
                    .shipperChargeAmount(calculated.shipperChargeAmount())
                    .driverPayoutAmount(calculated.driverPayoutAmount())
                    .tossFeeRate(calculated.tossFeeRate())
                    .tossFeeAmount(calculated.tossFeeAmount())
                    .platformGrossRevenue(calculated.platformGrossRevenue())
                    .platformNetRevenue(calculated.platformNetRevenue())
                    .negativeMargin(isNegative(calculated.platformNetRevenue()))
                    .policyConfigId(calculated.policyConfigId())
                    .policyUpdatedAt(calculated.policyUpdatedAt())
                    .build();
        }

        return FeeBreakdownPreviewResponse.builder()
                .previewMode(previewMode)
                .paymentProvider(paymentProvider)
                .baseAmount(calculated.baseAmount())
                .postTossBaseAmount(calculated.postTossBaseAmount())
                .shipperAppliedLevel(calculated.shipperAppliedLevel())
                .driverAppliedLevel(null)
                .shipperFeeRate(calculated.shipperFeeRate())
                .driverFeeRate(null)
                .shipperFeeAmount(calculated.shipperFeeAmount())
                .driverFeeAmount(null)
                .shipperPromoEligible(shipper.promoEligible())
                .driverPromoEligible(null)
                .shipperPromoApplied(calculated.shipperPromoApplied())
                .driverPromoApplied(null)
                .shipperMinFeeApplied(calculated.shipperMinFeeApplied())
                .driverMinFeeApplied(null)
                .shipperChargeAmount(calculated.shipperChargeAmount())
                .driverPayoutAmount(null)
                .tossFeeRate(calculated.tossFeeRate())
                .tossFeeAmount(calculated.tossFeeAmount())
                .platformGrossRevenue(calculated.platformGrossRevenue())
                .platformNetRevenue(calculated.platformNetRevenue())
                .negativeMargin(isNegative(calculated.platformNetRevenue()))
                .policyConfigId(calculated.policyConfigId())
                .policyUpdatedAt(calculated.policyUpdatedAt())
                .build();
    }

    private PreviewPayment resolvePreviewPayment(FeePreviewRequest request, PreviewContext context) {
        PaymentProvider provider = request == null ? null : request.getPaymentProvider();
        PayChannel requestedChannel = request == null ? null : request.getPayChannel();
        PaymentMethod requestedMethod = request == null ? null : request.getPaymentMethod();

        if (provider == null) {
            if (requestedChannel != null) {
                throw new IllegalArgumentException("payChannel can be used only with paymentProvider");
            }
            return new PreviewPayment(null, requestedMethod != null ? requestedMethod : context.defaultMethod(), null);
        }

        if (provider != PaymentProvider.TOSS) {
            throw new IllegalArgumentException("unsupported paymentProvider: " + provider);
        }

        PaymentMethod method = resolveTossMethod(requestedMethod != null ? requestedMethod : context.defaultMethod(), requestedChannel);
        PayChannel payChannel = resolvePayChannel(method, requestedChannel);
        return new PreviewPayment(provider, method, payChannel);
    }

    private boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private Long normalizeLevelBucket(Long level) {
        if (level == null || level <= 0L) {
            return 0L;
        }
        return Math.min(level, 3L);
    }

    private Long requiredUserId(Long userId, String fieldName) {
        if (userId == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return userId;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void validateNonNegative(BigDecimal amount, String fieldName) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0");
        }
    }

    private void requireAuthenticated(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private void validatePreviewRole(Users currentUser) {
        if (currentUser.getRole() != Role.SHIPPER && currentUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("only shipper or admin can preview fees");
        }
    }

    private void validateOrderAccess(Users currentUser, OrderPort.OrderSnapshot snap) {
        if (currentUser.getRole() != Role.SHIPPER) {
            return;
        }
        if (snap.shipperUserId() == null || !snap.shipperUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("shipper can preview only own order");
        }
    }

    private void validatePaymentPreviewOrderStatus(String orderStatus) {
        String status = normalizePaymentPreviewOrderStatus(orderStatus);
        boolean allowed = switch (status) {
            case "COMPLETED",
                    "PAID",
                    "CONFIRMED",
                    "DISPUTED",
                    "ADMIN_HOLD",
                    "ADMIN_FORCE_CONFIRMED",
                    "ADMIN_REJECTED" -> true;
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("fee preview for payment is available only after transport completed");
        }
    }

    private String normalizePaymentPreviewOrderStatus(String orderStatus) {
        String status = orderStatus == null ? "" : orderStatus.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETE".equals(status)) {
            return "COMPLETED";
        }
        return status;
    }

    private PaymentMethod resolveOrderPaymentMethod(String rawPayMethod) {
        String value = rawPayMethod == null ? "" : rawPayMethod.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains("CARD") || value.contains("TOSS") || value.contains("카드")) {
            return PaymentMethod.CARD;
        }
        if (value.contains("TRANSFER") || value.contains("계좌")) {
            return PaymentMethod.TRANSFER;
        }
        if (
                value.contains("CASH") ||
                        value.contains("현금") ||
                        value.contains("착불") ||
                        value.contains("선불") ||
                        value.contains("PREPAID") ||
                        value.contains("POSTPAID")
        ) {
            return PaymentMethod.CASH;
        }
        return null;
    }

    private PaymentMethod resolveTossMethod(PaymentMethod requestedMethod, PayChannel requestedChannel) {
        if (requestedChannel == PayChannel.APP_CARD || requestedChannel == PayChannel.CARD) {
            return PaymentMethod.CARD;
        }
        if (requestedChannel == PayChannel.TRANSFER) {
            return PaymentMethod.TRANSFER;
        }

        PaymentMethod method = requestedMethod == null ? PaymentMethod.CARD : requestedMethod;
        if (method == PaymentMethod.CASH) {
            throw new IllegalArgumentException("toss preview does not support CASH");
        }
        return method;
    }

    private PayChannel resolvePayChannel(PaymentMethod method, PayChannel requestedChannel) {
        if (requestedChannel != null) {
            return requestedChannel;
        }
        if (method == PaymentMethod.TRANSFER) {
            return PayChannel.TRANSFER;
        }
        return PayChannel.CARD;
    }

    private List<TransportPaymentStatus> paidOrConfirmedStatuses() {
        return List.of(
                TransportPaymentStatus.PAID,
                TransportPaymentStatus.CONFIRMED,
                TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
        );
    }

    private record PreviewContext(
            Long orderId,
            String previewMode,
            Long shipperUserId,
            Long driverUserId,
            BigDecimal baseAmount,
            PaymentMethod defaultMethod
    ) {
    }

    private record PreviewPayment(
            PaymentProvider provider,
            PaymentMethod method,
            PayChannel payChannel
    ) {
    }

    private record PreviewParticipant(
            Long userId,
            Long userLevel,
            Long appliedLevel,
            boolean promoEligible
    ) {
        private static PreviewParticipant from(PromotionEligibilityService.PromotionContext context) {
            return new PreviewParticipant(
                    context.userId(),
                    context.userLevel(),
                    MarketplaceFeeMath.normalizeLevelBucket(context.userLevel()),
                    context.promoEligible()
            );
        }
    }
}
