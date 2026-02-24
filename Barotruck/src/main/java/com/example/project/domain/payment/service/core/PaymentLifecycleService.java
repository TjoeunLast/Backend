package com.example.project.domain.payment.service.core;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.TransportPaymentStatus;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.ExternalPaymentClient;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentLifecycleService {

    private final ExternalPaymentClient externalPaymentClient;
    private final TransportPaymentRepository transportPaymentRepository;
    private final OrderPort orderPort;
    private final UserPort userPort;
    private final FeePolicyService feePolicyService;
    private final SettlementRepository settlementRepository;
    private final EntityManager entityManager;

    @Transactional
    public TransportPayment markPaid(
            Users currentUser,
            Long orderId,
            PaymentMethod method,
            PaymentTiming paymentTiming,
            String proofUrl,
            LocalDateTime paidAt
    ) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can mark paid");
        }

        OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(orderId);
        Long userLevel = userPort.getRequiredUser(snap.shipperUserId()).userLevel();
        boolean firstPaymentPromoEligible =
                transportPaymentRepository.countByShipperUserIdAndStatusIn(
                        snap.shipperUserId(),
                        paidOrConfirmedStatuses()
                ) == 0;

        FeePolicyService.FeeResult fee = feePolicyService.calculate(
                snap.amount(),
                userLevel,
                firstPaymentPromoEligible
        );

        TransportPayment transportPayment = transportPaymentRepository.findByOrderId(orderId)
                .orElseGet(() -> TransportPayment.ready(
                        orderId,
                        snap.shipperUserId(),
                        snap.driverUserId(),
                        snap.amount(),
                        fee.feeRate(),
                        fee.feeAmount(),
                        fee.netAmount(),
                        method,
                        resolvePaymentTiming(paymentTiming)
                ));

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        transportPayment.applyPaymentTiming(resolvePaymentTiming(paymentTiming));
        transportPayment.markPaid(proofUrl, paidAt);
        orderPort.setOrderPaid(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);
        upsertSettlementOnPaid(orderId, snap.shipperUserId(), snap.amount(), fee);
        return saved;
    }

    @Transactional
    public TransportPayment externalPay(
            Users currentUser,
            Long orderId,
            PaymentMethod method,
            PaymentTiming paymentTiming
    ) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can pay");
        }

        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("settlement not found. orderId=" + orderId));

        Long totalPriceLong = settlement.getTotalPrice();
        if (totalPriceLong == null || totalPriceLong <= 0) {
            throw new IllegalStateException("invalid TOTAL_PRICE. orderId=" + orderId);
        }

        BigDecimal totalPrice = BigDecimal.valueOf(totalPriceLong);
        ExternalPaymentClient.ExternalPayResult result =
                externalPaymentClient.pay("ORDER-" + orderId, totalPriceLong, method);

        if (!result.success()) {
            throw new IllegalStateException("external payment failed: " + result.failReason());
        }

        OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(orderId);
        Long userLevel = userPort.getRequiredUser(snap.shipperUserId()).userLevel();
        boolean firstPaymentPromoEligible =
                transportPaymentRepository.countByShipperUserIdAndStatusIn(
                        snap.shipperUserId(),
                        paidOrConfirmedStatuses()
                ) == 0;

        FeePolicyService.FeeResult fee = feePolicyService.calculate(
                totalPrice,
                userLevel,
                firstPaymentPromoEligible
        );

        TransportPayment transportPayment = transportPaymentRepository.findByOrderId(orderId)
                .orElseGet(() -> TransportPayment.ready(
                        orderId,
                        snap.shipperUserId(),
                        snap.driverUserId(),
                        totalPrice,
                        fee.feeRate(),
                        fee.feeAmount(),
                        fee.netAmount(),
                        method,
                        resolvePaymentTiming(paymentTiming)
                ));

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        transportPayment.applyPaymentTiming(resolvePaymentTiming(paymentTiming));
        transportPayment.markPaid(result.transactionId(), LocalDateTime.now());
        transportPayment.setPgTid(result.transactionId());
        orderPort.setOrderPaid(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);
        touchSettlementOnPaid(settlement);
        return saved;
    }

    @Transactional
    public TransportPayment confirmByDriver(Users currentUser, Long orderId) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.DRIVER) {
            throw new IllegalStateException("only driver can confirm");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));

        if (payment.getStatus() == TransportPaymentStatus.CONFIRMED
                || payment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return payment;
        }
        if (payment.getStatus() != TransportPaymentStatus.PAID) {
            throw new IllegalStateException("payment must be PAID before confirm");
        }

        payment.confirm(LocalDateTime.now());
        orderPort.setOrderConfirmed(orderId);

        TransportPayment saved = transportPaymentRepository.save(payment);
        completeSettlementOnConfirm(orderId);
        return saved;
    }

    @Transactional
    public TransportPayment applyPaidFromGatewayTx(PaymentGatewayTransaction tx) {
        OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(tx.getOrderId());
        if (snap.driverUserId() == null) {
            throw new IllegalStateException("assigned driver not found");
        }
        if (snap.shipperUserId() == null || !snap.shipperUserId().equals(tx.getShipperUserId())) {
            throw new IllegalStateException("gateway transaction shipper mismatch");
        }

        Long userLevel = userPort.getRequiredUser(snap.shipperUserId()).userLevel();
        boolean firstPaymentPromoEligible =
                transportPaymentRepository.countByShipperUserIdAndStatusIn(
                        snap.shipperUserId(),
                        paidOrConfirmedStatuses()
                ) == 0;
        FeePolicyService.FeeResult fee = feePolicyService.calculate(
                tx.getAmount(),
                userLevel,
                firstPaymentPromoEligible
        );

        TransportPayment payment = transportPaymentRepository.findByOrderId(tx.getOrderId())
                .orElseGet(() -> TransportPayment.ready(
                        tx.getOrderId(),
                        snap.shipperUserId(),
                        snap.driverUserId(),
                        tx.getAmount(),
                        fee.feeRate(),
                        fee.feeAmount(),
                        fee.netAmount(),
                        tx.getMethod(),
                        resolveGatewayPaymentTiming(tx)
                ));

        payment.applyPaymentTiming(resolveGatewayPaymentTiming(tx));

        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            throw new IllegalStateException("cannot mark paid for cancelled payment");
        }
        if (payment.getStatus() == TransportPaymentStatus.PAID
                || payment.getStatus() == TransportPaymentStatus.CONFIRMED
                || payment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
                || payment.getStatus() == TransportPaymentStatus.DISPUTED
                || payment.getStatus() == TransportPaymentStatus.ADMIN_HOLD
                || payment.getStatus() == TransportPaymentStatus.ADMIN_REJECTED) {
            return payment;
        }

        String pgReference = firstNonBlank(tx.getTransactionId(), tx.getPaymentKey(), tx.getPgOrderId());
        payment.markPaid(pgReference, LocalDateTime.now());
        payment.setPgTid(pgReference);
        orderPort.setOrderPaid(tx.getOrderId());

        TransportPayment saved = transportPaymentRepository.save(payment);
        upsertSettlementOnPaid(tx.getOrderId(), snap.shipperUserId(), tx.getAmount(), fee);
        return saved;
    }

    private void upsertSettlementOnPaid(Long orderId, Long shipperUserId, BigDecimal grossAmount, FeePolicyService.FeeResult fee) {
        long totalPrice = grossAmount.longValue();
        long feeRatePercent = fee.feeRate().multiply(new BigDecimal("100")).longValue();

        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Order orderRef = entityManager.getReference(Order.class, orderId);
                    Users userRef = entityManager.getReference(Users.class, shipperUserId);
                    return Settlement.builder()
                            .order(orderRef)
                            .user(userRef)
                            .build();
                });

        settlement.setLevelDiscount(0L);
        settlement.setCouponDiscount(0L);
        settlement.setTotalPrice(totalPrice);
        settlement.setFeeRate(feeRatePercent);
        settlement.setStatus("READY");
        settlement.setFeeDate(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    private void touchSettlementOnPaid(Settlement settlement) {
        if (settlement.getFeeDate() == null) {
            settlement.setFeeDate(LocalDateTime.now());
        }
        settlement.setStatus("READY");
        settlementRepository.save(settlement);
    }

    private void completeSettlementOnConfirm(Long orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("settlement not found. orderId=" + orderId));

        settlement.setStatus("COMPLETED");
        settlement.setFeeCompleteDate(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    private void requireAuthenticated(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private List<TransportPaymentStatus> paidOrConfirmedStatuses() {
        return List.of(
                TransportPaymentStatus.PAID,
                TransportPaymentStatus.CONFIRMED,
                TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
        );
    }

    private PaymentTiming resolvePaymentTiming(PaymentTiming paymentTiming) {
        return paymentTiming == null ? PaymentTiming.PREPAID : paymentTiming;
    }

    private PaymentTiming resolveGatewayPaymentTiming(PaymentGatewayTransaction tx) {
        return PaymentTiming.POSTPAID;
    }
}

