package com.example.project.domain.payment.service.core;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentRequest.UpdateTransportPaymentStatusRequest;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminTransportPaymentStatusService {

    private final PaymentLifecycleService paymentLifecycleService;
    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentDisputeRepository paymentDisputeRepository;
    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final OrderPort orderPort;
    private final DriverPayoutService driverPayoutService;

    @Transactional
    public TransportPayment updateStatus(
            Users currentUser,
            Long orderId,
            UpdateTransportPaymentStatusRequest request
    ) {
        requireAuthenticated(currentUser);
        requireAdmin(currentUser);
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }

        TransportPayment payment = ensurePayment(orderId);
        Settlement settlement = ensureSettlement(orderId, payment);
        PaymentDispute dispute = paymentDisputeRepository.findByOrderId(orderId).orElse(null);

        applyPaymentMetadata(payment, request);
        String adminMemo = normalize(request.getAdminMemo());
        if (settlement.getFeeDate() == null) {
            settlement.setFeeDate(LocalDateTime.now());
        }
        settlement.setField5(adminMemo);
        syncSettlementSnapshots(settlement, payment);

        PaymentDispute updatedDispute = applyStatusTransition(
                orderId,
                payment,
                settlement,
                dispute,
                currentUser,
                request,
                adminMemo
        );

        transportPaymentRepository.save(payment);
        settlementRepository.save(settlement);
        if (updatedDispute != null) {
            paymentDisputeRepository.save(updatedDispute);
        }
        if (shouldTriggerAutoPayout(request.getStatus())) {
            driverPayoutService.tryAutoRequestPayoutForOrder(
                    orderId,
                    "ADMIN_PAYMENT_STATUS_" + request.getStatus().name()
            );
        }
        return payment;
    }

    private PaymentDispute applyStatusTransition(
            Long orderId,
            TransportPayment payment,
            Settlement settlement,
            PaymentDispute dispute,
            Users currentUser,
            UpdateTransportPaymentStatusRequest request,
            String adminMemo
    ) {
        return switch (request.getStatus()) {
            case READY -> {
                payment.resetToReady();
                applyReadySettlement(settlement);
                yield dispute;
            }
            case PAID -> {
                markPaid(payment, request);
                applyReadySettlement(settlement);
                orderPort.setOrderPaid(orderId);
                yield dispute;
            }
            case CONFIRMED -> {
                confirmPayment(payment, request);
                applyCompletedSettlement(settlement, request.getConfirmedAt());
                orderPort.setOrderConfirmed(orderId);
                yield dispute;
            }
            case DISPUTED -> {
                markPaid(payment, request);
                payment.updateStatus(TransportPaymentStatus.DISPUTED);
                applyWaitingSettlement(settlement);
                orderPort.setOrderDisputed(orderId);
                yield upsertDispute(payment, dispute, currentUser, PaymentDisputeStatus.PENDING, adminMemo);
            }
            case ADMIN_HOLD -> {
                markPaid(payment, request);
                payment.updateStatus(TransportPaymentStatus.ADMIN_HOLD);
                applyWaitingSettlement(settlement);
                orderPort.setOrderDisputed(orderId);
                yield upsertDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_HOLD, adminMemo);
            }
            case ADMIN_FORCE_CONFIRMED -> {
                confirmPayment(payment, request);
                payment.updateStatus(TransportPaymentStatus.ADMIN_FORCE_CONFIRMED);
                applyCompletedSettlement(settlement, request.getConfirmedAt());
                orderPort.setOrderConfirmed(orderId);
                yield upsertDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED, adminMemo);
            }
            case ADMIN_REJECTED -> {
                markPaid(payment, request);
                payment.updateStatus(TransportPaymentStatus.ADMIN_REJECTED);
                applyWaitingSettlement(settlement);
                orderPort.setOrderDisputed(orderId);
                yield upsertDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_REJECTED, adminMemo);
            }
            case CANCELLED -> {
                payment.cancel();
                applyReadySettlement(settlement);
                yield dispute;
            }
        };
    }

    private TransportPayment ensurePayment(Long orderId) {
        return paymentLifecycleService.requireInitializedPayment(orderId);
    }

    private Settlement ensureSettlement(Long orderId, TransportPayment payment) {
        Settlement settlement = settlementRepository.findByOrderId(orderId).orElse(null);
        if (settlement != null) {
            return settlement;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));

        return Settlement.builder()
                .order(order)
                .user(order.getUser())
                .levelDiscount(0L)
                .couponDiscount(0L)
                .totalPrice(toLong(payment.getAmount()))
                .feeRate(toPercentage(payment.getFeeRateSnapshot()))
                .status(SettlementStatus.READY)
                .feeDate(LocalDateTime.now())
                .build();
    }

    private void applyPaymentMetadata(
            TransportPayment payment,
            UpdateTransportPaymentStatusRequest request
    ) {
        payment.applyMethod(resolvePaymentMethod(request.getMethod(), payment));
        payment.applyPaymentTiming(resolvePaymentTiming(request.getPaymentTiming(), payment));
    }

    private void markPaid(
            TransportPayment payment,
            UpdateTransportPaymentStatusRequest request
    ) {
        payment.markPaid(
                resolveProofUrl(request.getProofUrl(), payment),
                resolvePaidAt(request.getPaidAt(), payment)
        );
    }

    private void confirmPayment(
            TransportPayment payment,
            UpdateTransportPaymentStatusRequest request
    ) {
        markPaid(payment, request);
        payment.confirm(resolveConfirmedAt(request.getConfirmedAt(), payment));
    }

    private PaymentDispute upsertDispute(
            TransportPayment payment,
            PaymentDispute dispute,
            Users currentUser,
            PaymentDisputeStatus nextStatus,
            String adminMemo
    ) {
        PaymentDispute target = dispute;
        if (target == null) {
            Long requesterUserId = payment.getDriverUserId() != null
                    ? payment.getDriverUserId()
                    : currentUser.getUserId();
            target = PaymentDispute.create(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    requesterUserId,
                    currentUser.getUserId(),
                    PaymentDisputeReason.OTHER,
                    buildDisputeDescription(nextStatus, adminMemo),
                    payment.getProofUrl()
            );
        }
        target.updateStatus(nextStatus, adminMemo);
        return target;
    }

    private void applyReadySettlement(Settlement settlement) {
        settlement.setStatus(SettlementStatus.READY);
        settlement.setFeeCompleteDate(null);
    }

    private void applyWaitingSettlement(Settlement settlement) {
        settlement.setStatus(SettlementStatus.WAIT);
        settlement.setFeeCompleteDate(null);
    }

    private void applyCompletedSettlement(
            Settlement settlement,
            LocalDateTime confirmedAt
    ) {
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setFeeCompleteDate(confirmedAt != null ? confirmedAt : LocalDateTime.now());
    }

    private void syncSettlementSnapshots(
            Settlement settlement,
            TransportPayment payment
    ) {
        if (settlement.getLevelDiscount() == null) {
            settlement.setLevelDiscount(0L);
        }
        if (settlement.getCouponDiscount() == null) {
            settlement.setCouponDiscount(0L);
        }
        if (payment.getAmount() != null) {
            settlement.setTotalPrice(toLong(payment.getAmount()));
        }
        if (payment.getFeeRateSnapshot() != null) {
            settlement.setFeeRate(toPercentage(payment.getFeeRateSnapshot()));
        }
    }

    private boolean shouldTriggerAutoPayout(TransportPaymentStatus status) {
        return status == TransportPaymentStatus.CONFIRMED
                || status == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED;
    }

    private PaymentMethod resolvePaymentMethod(
            PaymentMethod requestedMethod,
            TransportPayment payment
    ) {
        if (requestedMethod != null) {
            return requestedMethod;
        }
        if (payment.getMethod() != null) {
            return payment.getMethod();
        }
        return PaymentMethod.TRANSFER;
    }

    private PaymentTiming resolvePaymentTiming(
            PaymentTiming requestedTiming,
            TransportPayment payment
    ) {
        if (requestedTiming != null) {
            return requestedTiming;
        }
        if (payment.getPaymentTiming() != null) {
            return payment.getPaymentTiming();
        }
        return PaymentTiming.PREPAID;
    }

    private LocalDateTime resolvePaidAt(
            LocalDateTime requestedPaidAt,
            TransportPayment payment
    ) {
        if (requestedPaidAt != null) {
            return requestedPaidAt;
        }
        if (payment.getPaidAt() != null) {
            return payment.getPaidAt();
        }
        return LocalDateTime.now();
    }

    private LocalDateTime resolveConfirmedAt(
            LocalDateTime requestedConfirmedAt,
            TransportPayment payment
    ) {
        if (requestedConfirmedAt != null) {
            return requestedConfirmedAt;
        }
        if (payment.getConfirmedAt() != null) {
            return payment.getConfirmedAt();
        }
        return LocalDateTime.now();
    }

    private String resolveProofUrl(
            String requestedProofUrl,
            TransportPayment payment
    ) {
        String normalized = normalize(requestedProofUrl);
        if (normalized != null) {
            return normalized;
        }
        return normalize(payment.getProofUrl());
    }

    private String buildDisputeDescription(
            PaymentDisputeStatus nextStatus,
            String adminMemo
    ) {
        if (adminMemo != null && !adminMemo.isBlank()) {
            return adminMemo;
        }
        return "ADMIN payment status update: " + nextStatus.name();
    }

    private void requireAuthenticated(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private void requireAdmin(Users currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("only admin can update transport payment status");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long toLong(BigDecimal value) {
        if (value == null) {
            return 0L;
        }
        return value.longValue();
    }

    private Long toPercentage(BigDecimal feeRateSnapshot) {
        if (feeRateSnapshot == null) {
            return 0L;
        }
        return feeRateSnapshot.multiply(new BigDecimal("100")).longValue();
    }
}
