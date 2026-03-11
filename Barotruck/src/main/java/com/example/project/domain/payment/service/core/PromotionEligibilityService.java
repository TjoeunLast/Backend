package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionEligibilityService {

    private final OrderPort orderPort;
    private final UserPort userPort;
    private final TransportPaymentRepository transportPaymentRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;

    @Transactional(readOnly = true)
    public PromotionContext getShipperFirstPaymentContext(Long orderId) {
        OrderPort.OrderSnapshot snapshot = orderPort.getRequiredSnapshot(orderId);
        Long shipperUserId = requireUserId(snapshot.shipperUserId(), "shipper");
        UserPort.UserInfo user = userPort.getRequiredUser(shipperUserId);
        TransportPayment currentPayment = transportPaymentRepository.findByOrderId(orderId).orElse(null);

        boolean promoEligible = isCurrentShipperPromoApplied(currentPayment)
                || !transportPaymentRepository.existsByShipperUserIdAndFirstPaymentPromoAppliedTrueAndOrderIdNot(
                        shipperUserId,
                        orderId
                );

        return new PromotionContext(shipperUserId, user.userLevel(), promoEligible);
    }

    @Transactional
    public PromotionContext resolveShipperFirstPaymentContext(TransportPayment payment) {
        if (payment == null || payment.getOrderId() == null) {
            throw new IllegalArgumentException("transport payment is required");
        }

        Long shipperUserId = requireUserId(payment.getShipperUserId(), "shipper");
        UserPort.UserInfo user = userPort.lockRequiredUser(shipperUserId);

        if (payment.isFirstPaymentPromoApplied()) {
            return new PromotionContext(shipperUserId, user.userLevel(), true);
        }

        boolean promoEligible = !transportPaymentRepository.existsByShipperUserIdAndFirstPaymentPromoAppliedTrueAndOrderIdNot(
                shipperUserId,
                payment.getOrderId()
        );
        payment.applyFirstPaymentPromo(promoEligible);
        return new PromotionContext(shipperUserId, user.userLevel(), promoEligible);
    }

    @Transactional(readOnly = true)
    public PromotionContext getDriverFirstTransportContext(Long orderId) {
        OrderPort.OrderSnapshot snapshot = orderPort.getRequiredSnapshot(orderId);
        Long driverUserId = requireUserId(snapshot.driverUserId(), "driver");
        UserPort.UserInfo user = userPort.getRequiredUser(driverUserId);
        DriverPayoutItem currentItem = driverPayoutItemRepository.findByOrderId(orderId).orElse(null);

        boolean promoEligible = isCurrentDriverPromoApplied(currentItem)
                || !driverPayoutItemRepository.existsByDriverUserIdAndFirstTransportPromoAppliedTrueAndOrderIdNot(
                        driverUserId,
                        orderId
                );

        return new PromotionContext(driverUserId, user.userLevel(), promoEligible);
    }

    @Transactional
    public PromotionContext applyDriverFirstTransportPromotion(DriverPayoutItem payoutItem) {
        if (payoutItem == null || payoutItem.getOrderId() == null) {
            throw new IllegalArgumentException("driver payout item is required");
        }

        Long driverUserId = requireUserId(payoutItem.getDriverUserId(), "driver");
        UserPort.UserInfo user = userPort.lockRequiredUser(driverUserId);

        if (payoutItem.isFirstTransportPromoApplied()) {
            return new PromotionContext(driverUserId, user.userLevel(), true);
        }

        boolean promoEligible = !driverPayoutItemRepository.existsByDriverUserIdAndFirstTransportPromoAppliedTrueAndOrderIdNot(
                driverUserId,
                payoutItem.getOrderId()
        );
        payoutItem.applyFirstTransportPromo(promoEligible);
        return new PromotionContext(driverUserId, user.userLevel(), promoEligible);
    }

    private boolean isCurrentShipperPromoApplied(TransportPayment payment) {
        return payment != null && payment.isFirstPaymentPromoApplied();
    }

    private boolean isCurrentDriverPromoApplied(DriverPayoutItem payoutItem) {
        return payoutItem != null && payoutItem.isFirstTransportPromoApplied();
    }

    private Long requireUserId(Long userId, String userType) {
        if (userId == null) {
            throw new IllegalStateException(userType + " user not found");
        }
        return userId;
    }

    public record PromotionContext(
            Long userId,
            Long userLevel,
            boolean promoEligible
    ) {
    }
}
