package com.example.project.domain.payment.service.core;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentLifecycleService {

    private final TransportPaymentRepository transportPaymentRepository;
    private final OrderPort orderPort;
    private final UserPort userPort;
    private final FeePolicyService feePolicyService;
    private final SettlementRepository settlementRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;
    private final EntityManager entityManager;
    private final NotificationService notificationService;

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
        validateShipperOwnership(currentUser, snap);
        validatePaymentStartOrderStatus(snap.status());
        if (snap.driverUserId() == null) {
            throw new IllegalStateException("assigned driver not found");
        }

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

        TransportPayment transportPayment = requireInitializedPayment(orderId);
        PaymentMethod effectiveMethod = resolveManualPaymentMethod(method, transportPayment);
        validateManualPaymentProof(effectiveMethod, proofUrl);
        transportPayment.applyPricingSnapshots(
                fee.chargedAmount(),
                fee.feeRate(),
                fee.feeAmount(),
                fee.driverAmount()
        );
        boolean shouldNotifyDriver = transportPayment.getStatus() != TransportPaymentStatus.PAID
                && transportPayment.getStatus() != TransportPaymentStatus.CONFIRMED
                && transportPayment.getStatus() != TransportPaymentStatus.ADMIN_FORCE_CONFIRMED;

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        transportPayment.applyPaymentTiming(resolvePaymentTiming(paymentTiming));
        transportPayment.markPaid(proofUrl, paidAt);
        orderPort.setOrderPaid(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);
        upsertSettlementOnPaid(orderId, snap.shipperUserId(), snap.amount(), fee);
        if (shouldNotifyDriver) {
            sendPaymentNotificationSafely(
                    snap.driverUserId(),
                    "화주 결제 완료",
                    "화주가 결제를 완료했습니다. 최종 확인을 진행해주세요.",
                    orderId
            );
        }
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
        if (payment.getDriverUserId() == null || !payment.getDriverUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("driver can confirm only own assigned payment");
        }

        payment.confirm(LocalDateTime.now());
        orderPort.setOrderConfirmed(orderId);

        TransportPayment saved = transportPaymentRepository.save(payment);
        completeSettlementOnConfirm(orderId);
        sendPaymentNotificationSafely(
                payment.getShipperUserId(),
                "정산 확인 완료",
                "차주가 결제를 최종 확인했습니다.",
                orderId
        );
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
        validatePaymentStartOrderStatus(snap.status());

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

        TransportPayment payment = requireInitializedPayment(tx.getOrderId());

        payment.applyPaymentTiming(resolveGatewayPaymentTiming(tx));
        payment.applyPricingSnapshots(
                tx.getAmount(),
                fee.feeRate(),
                fee.feeAmount(),
                fee.driverAmount()
        );

        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            throw new IllegalStateException("cannot mark paid for cancelled payment");
        }
        if (payment.getStatus() == TransportPaymentStatus.CONFIRMED
                || payment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
                || payment.getStatus() == TransportPaymentStatus.DISPUTED
                || payment.getStatus() == TransportPaymentStatus.ADMIN_HOLD
                || payment.getStatus() == TransportPaymentStatus.ADMIN_REJECTED) {
            return payment;
        }
        if (payment.getStatus() == TransportPaymentStatus.PAID) {
            upsertSettlementOnPaid(tx.getOrderId(), snap.shipperUserId(), snap.amount(), fee);
            orderPort.setOrderPaid(tx.getOrderId());
            return payment;
        }

        String pgReference = firstNonBlank(tx.getTransactionId(), tx.getPaymentKey(), tx.getPgOrderId());
        payment.markPaid(pgReference, LocalDateTime.now());
        payment.setPgTid(pgReference);
        orderPort.setOrderPaid(tx.getOrderId());

        TransportPayment saved = transportPaymentRepository.save(payment);
        upsertSettlementOnPaid(tx.getOrderId(), snap.shipperUserId(), snap.amount(), fee);
        sendPaymentNotificationSafely(
                snap.driverUserId(),
                "화주 결제 완료",
                "화주가 결제를 완료했습니다. 최종 확인을 진행해주세요.",
                tx.getOrderId()
        );
        return saved;
    }

    @Transactional
    public TransportPayment ensureReadyPaymentRecord(Long orderId) {
        OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(orderId);
        validatePaymentStartOrderStatus(snap.status());
        if (snap.driverUserId() == null) {
            throw new IllegalStateException("assigned driver not found");
        }

        TransportPayment existing = transportPaymentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return existing;
        }

        PaymentMethod orderMethod = resolveOrderPaymentMethod(snap.payMethod());
        if (orderMethod == null) {
            return null;
        }

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

        TransportPayment readyPayment = TransportPayment.ready(
                orderId,
                snap.shipperUserId(),
                snap.driverUserId(),
                fee.chargedAmount(),
                fee.feeRate(),
                fee.feeAmount(),
                fee.driverAmount(),
                orderMethod,
                PaymentTiming.POSTPAID
        );

        return transportPaymentRepository.save(readyPayment);
    }

    @Transactional
    public TransportPayment requireInitializedPayment(Long orderId) {
        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            return payment;
        }

        TransportPayment initialized = ensureReadyPaymentRecord(orderId);
        if (initialized == null) {
            throw new IllegalStateException("transport payment could not be initialized. unsupported payMethod. orderId=" + orderId);
        }
        return initialized;
    }

    @Transactional
    public TransportPayment applyCanceledFromGatewayTx(PaymentGatewayTransaction tx) {
        if (tx == null) {
            throw new IllegalArgumentException("gateway transaction is required");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(tx.getOrderId()).orElse(null);
        if (payment == null) {
            throw new IllegalStateException("transport payment not found. orderId=" + tx.getOrderId());
        }
        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            return payment;
        }
        if (driverPayoutItemRepository.findByOrderId(tx.getOrderId()).isPresent()) {
            return payment;
        }
        if (payment.getStatus() != TransportPaymentStatus.PAID && payment.getStatus() != TransportPaymentStatus.READY) {
            return payment;
        }

        payment.cancel();
        TransportPayment saved = transportPaymentRepository.save(payment);
        settlementRepository.findByOrderId(tx.getOrderId()).ifPresent(settlement -> {
            if (settlement.getStatus() != SettlementStatus.WAIT) {
                settlement.setStatus(SettlementStatus.READY);
                settlement.setFeeCompleteDate(null);
            }
            settlement.setField5(defaultIfBlank(tx.getCancelReason(), settlement.getField5()));
            settlementRepository.save(settlement);
        });
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
        settlement.setStatus(SettlementStatus.READY);
        settlement.setFeeDate(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    private void completeSettlementOnConfirm(Long orderId) {
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("settlement not found. orderId=" + orderId));

        settlement.setStatus(SettlementStatus.COMPLETED);
        settlementRepository.save(settlement);
    }

    private void sendPaymentNotificationSafely(Long recipientUserId, String title, String body, Long targetId) {
        if (recipientUserId == null) {
            return;
        }
        try {
            notificationService.sendNotification(recipientUserId, "PAYMENT", title, body, targetId);
        } catch (Exception e) {
            System.out.println("결제 알림 발송 중 예외 발생: " + e.getMessage());
        }
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
        if (value.contains("RECEIPT") || value.contains("영수증") || value.contains("MONTH") || value.contains("월말")) {
            return null;
        }
        return PaymentMethod.CASH;
    }

    private PaymentMethod resolveManualPaymentMethod(PaymentMethod requestedMethod, TransportPayment existing) {
        if (requestedMethod != null) {
            return requestedMethod;
        }
        if (existing != null && existing.getMethod() != null) {
            return existing.getMethod();
        }
        return PaymentMethod.TRANSFER;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private void validateManualPaymentProof(PaymentMethod method, String proofUrl) {
        if (method != PaymentMethod.TRANSFER) {
            return;
        }
        if (proofUrl == null || proofUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("proofUrl is required for TRANSFER payment");
        }
    }

    private void validateShipperOwnership(Users currentUser, OrderPort.OrderSnapshot snap) {
        if (snap.shipperUserId() == null || !snap.shipperUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("shipper can pay only own order");
        }
    }

    private void validatePaymentStartOrderStatus(String orderStatus) {
        String status = normalizePaymentStartOrderStatus(orderStatus);
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
            throw new IllegalStateException("payment can start only after transport completed");
        }
    }

    private String normalizePaymentStartOrderStatus(String orderStatus) {
        String status = orderStatus == null ? "" : orderStatus.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETE".equals(status)) {
            return "COMPLETED";
        }
        return status;
    }
}

