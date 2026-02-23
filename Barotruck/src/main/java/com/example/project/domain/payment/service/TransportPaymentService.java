package com.example.project.domain.payment.service;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.payment.domain.*;
import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
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
public class TransportPaymentService {

    private final ExternalPaymentClient externalPaymentClient;

    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentDisputeRepository paymentDisputeRepository;
    private final OrderPort orderPort;
    private final UserPort userPort;
    private final FeePolicyService feePolicyService;

    private final SettlementRepository settlementRepository;
    private final EntityManager entityManager;

    /*
     * 기존(수기/입금증 등) 결제 처리
     * - 금액은 OrderSnapshot.amount() 기준
     * - 결제완료 시 Settlement를 생성/업데이트하면서 totalPrice를 grossAmount로 세팅함
     */
    @Transactional
    public TransportPayment markPaid(
            Users currentUser,
            Long orderId,
            PaymentMethod method,
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
                        method
                ));

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        transportPayment.markPaid(proofUrl, paidAt);
        orderPort.setOrderPaid(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);

        // Settlement 생성/업데이트 (totalPrice를 grossAmount로 세팅)
        upsertSettlementOnPaid(orderId, currentUser, snap.amount(), fee);

        return saved;
    }

    /*
     * 외부 결제 처리 (TOTAL_PRICE 기반)
     * - 금액은 Settlement.totalPrice(TOTAL_PRICE)를 소스 오브 트루스로 사용
     * - 결제 성공 시 TransportPayment를 PAID로 변경하고, Settlement의 totalPrice는 덮어쓰지 않음
     */
    @Transactional
    public TransportPayment externalPay(
            Users currentUser,
            Long orderId,
            PaymentMethod method
    ) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can pay");
        }

        // 1) TOTAL_PRICE 조회 (외부 결제 금액)
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("settlement not found. orderId=" + orderId));

        Long totalPriceLong = settlement.getTotalPrice();
        if (totalPriceLong == null || totalPriceLong <= 0) {
            throw new IllegalStateException("invalid TOTAL_PRICE. orderId=" + orderId);
        }

        BigDecimal totalPrice = BigDecimal.valueOf(totalPriceLong);

        // 2) 외부 결제 요청
        ExternalPaymentClient.ExternalPayResult result =
                externalPaymentClient.pay("ORDER-" + orderId, totalPriceLong, method);

        if (!result.success()) {
            throw new IllegalStateException("external payment failed: " + result.failReason());
        }

        // 3) 수수료 계산 (TOTAL_PRICE 기준)
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

        // 4) TransportPayment 생성/갱신 (금액은 TOTAL_PRICE)
        TransportPayment transportPayment = transportPaymentRepository.findByOrderId(orderId)
                .orElseGet(() -> TransportPayment.ready(
                        orderId,
                        snap.shipperUserId(),
                        snap.driverUserId(),
                        totalPrice,
                        fee.feeRate(),
                        fee.feeAmount(),
                        fee.netAmount(),
                        method
                ));

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        // proofUrl 파라미터가 없는 흐름이라, transactionId를 proofUrl 자리에 넣어서 추적(임시)
        transportPayment.markPaid(result.transactionId(), LocalDateTime.now());
        orderPort.setOrderPaid(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);

        // 5) Settlement는 totalPrice는 유지하고 상태/일시만 보정
        touchSettlementOnPaid(settlement);

        return saved;
    }

    @Transactional
    public TransportPayment confirm(Users currentUser, Long orderId) {
        requireAuthenticated(currentUser);

        TransportPayment transportPayment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));
        if (currentUser.getRole() != Role.DRIVER) {
            throw new IllegalStateException("only driver can confirm");
        }

        if (transportPayment.getStatus() == TransportPaymentStatus.CONFIRMED
                || transportPayment.getStatus() == TransportPaymentStatus.ADMIN_FORCE_CONFIRMED) {
            return transportPayment;
        }

        if (transportPayment.getStatus() != TransportPaymentStatus.PAID) {
            throw new IllegalStateException("payment must be PAID before confirm");
        }

        transportPayment.confirm(LocalDateTime.now());
        orderPort.setOrderConfirmed(orderId);

        TransportPayment saved = transportPaymentRepository.save(transportPayment);

        // 정산 완료 처리
        completeSettlementOnConfirm(orderId);

        return saved;
    }

    /*
     * 분쟁 처리 (dispute)
     * - PaymentController에 이미 /dispute 가 있으므로 반드시 존재해야 함
     * - 화주/차주 둘 다 가능하게 열어두고, 최소 상태 체크만 둠
     */
    @Transactional
    public TransportPayment dispute(Users currentUser, Long orderId) {
        createDisputeInternal(
                currentUser,
                orderId,
                null,
                PaymentDisputeReason.OTHER,
                "legacy dispute request",
                null,
                true
        );
        return transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));
    }

    @Transactional
    public PaymentDispute createDispute(Users currentUser, Long orderId, CreatePaymentDisputeRequest request) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("only admin can create dispute");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        return createDisputeInternal(
                currentUser,
                orderId,
                request.getRequesterUserId(),
                request.getReasonCode(),
                request.getDescription(),
                request.getAttachmentUrl(),
                false
        );
    }

    @Transactional
    public PaymentDispute updateDisputeStatus(
            Users currentUser,
            Long orderId,
            Long disputeId,
            UpdatePaymentDisputeStatusRequest request
    ) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("only admin can update dispute status");
        }
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (request.getStatus() == PaymentDisputeStatus.PENDING) {
            throw new IllegalStateException("cannot change status back to PENDING");
        }

        PaymentDispute dispute = paymentDisputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("payment dispute not found. disputeId=" + disputeId));
        if (!dispute.getOrderId().equals(orderId)) {
            throw new IllegalStateException("dispute/order mismatch");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));

        PaymentDisputeStatus nextStatus = request.getStatus();
        dispute.updateStatus(nextStatus, normalize(request.getAdminMemo()));
        TransportPaymentStatus mappedStatus = mapDisputeStatusToPaymentStatus(nextStatus);

        if (nextStatus == PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED && payment.getConfirmedAt() == null) {
            payment.confirm(LocalDateTime.now());
        }
        payment.updateStatus(mappedStatus);
        transportPaymentRepository.save(payment);

        if (nextStatus == PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED) {
            updateOrderStatusSafely(orderId, true);
        } else {
            updateOrderStatusSafely(orderId, false);
        }

        settlementRepository.findByOrderId(orderId).ifPresent(s -> {
            s.setStatus(nextStatus.name());
            if (nextStatus == PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED) {
                s.setFeeCompleteDate(LocalDateTime.now());
            }
            settlementRepository.save(s);
        });

        return paymentDisputeRepository.save(dispute);
    }

    private PaymentDispute createDisputeInternal(
            Users currentUser,
            Long orderId,
            Long requesterUserIdFromRequest,
            PaymentDisputeReason reasonCode,
            String description,
            String attachmentUrl,
            boolean allowLegacyShipper
    ) {
        requireAuthenticated(currentUser);
        if (reasonCode == null) {
            throw new IllegalArgumentException("reasonCode is required");
        }

        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("transport payment not found. orderId=" + orderId));

        validateDisputableStatus(payment);

        Long requesterUserId = resolveRequesterUserId(
                currentUser,
                payment,
                requesterUserIdFromRequest,
                allowLegacyShipper
        );

        PaymentDispute existing = paymentDisputeRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return existing;
        }

        String requiredDescription = normalizeRequired(description, "description is required");
        PaymentDispute dispute = PaymentDispute.create(
                orderId,
                payment.getPaymentId(),
                requesterUserId,
                currentUser.getUserId(),
                reasonCode,
                requiredDescription,
                normalize(attachmentUrl)
        );
        PaymentDispute saved = paymentDisputeRepository.save(dispute);

        applyDisputedState(orderId, payment);
        return saved;
    }

    private void applyDisputedState(Long orderId, TransportPayment payment) {
        payment.dispute();
        transportPaymentRepository.save(payment);
        updateOrderStatusSafely(orderId, false);

        // settlement도 분쟁 상태로 표시(STATUS 컬럼이 String이라 확장 가능)
        settlementRepository.findByOrderId(orderId).ifPresent(s -> {
            s.setStatus(TransportPaymentStatus.DISPUTED.name());
            settlementRepository.save(s);
        });
    }

    private Long resolveRequesterUserId(
            Users currentUser,
            TransportPayment payment,
            Long requesterUserIdFromRequest,
            boolean allowLegacyShipper
    ) {
        if (currentUser.getRole() == Role.DRIVER) {
            if (!currentUser.getUserId().equals(payment.getDriverUserId())) {
                throw new IllegalStateException("driver can dispute only own assigned payment");
            }
            if (requesterUserIdFromRequest != null && !requesterUserIdFromRequest.equals(currentUser.getUserId())) {
                throw new IllegalStateException("driver cannot proxy requesterUserId");
            }
            return currentUser.getUserId();
        }

        if (currentUser.getRole() == Role.ADMIN) {
            if (requesterUserIdFromRequest == null) {
                throw new IllegalArgumentException("admin must provide requesterUserId");
            }
            if (payment.getDriverUserId() == null) {
                throw new IllegalStateException("assigned driver not found");
            }
            if (!requesterUserIdFromRequest.equals(payment.getDriverUserId())) {
                throw new IllegalStateException("requesterUserId must be assigned driver");
            }
            return requesterUserIdFromRequest;
        }

        if (allowLegacyShipper && currentUser.getRole() == Role.SHIPPER) {
            if (!currentUser.getUserId().equals(payment.getShipperUserId())) {
                throw new IllegalStateException("shipper can dispute only own payment");
            }
            return currentUser.getUserId();
        }

        throw new IllegalStateException("only driver/admin can create dispute");
    }

    private void validateDisputableStatus(TransportPayment payment) {
        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            throw new IllegalStateException("cannot dispute cancelled payment");
        }
        if (payment.getStatus() == TransportPaymentStatus.READY) {
            throw new IllegalStateException("cannot dispute before paid");
        }
    }

    private TransportPaymentStatus mapDisputeStatusToPaymentStatus(PaymentDisputeStatus status) {
        return switch (status) {
            case PENDING -> TransportPaymentStatus.DISPUTED;
            case ADMIN_HOLD -> TransportPaymentStatus.ADMIN_HOLD;
            case ADMIN_FORCE_CONFIRMED -> TransportPaymentStatus.ADMIN_FORCE_CONFIRMED;
            case ADMIN_REJECTED -> TransportPaymentStatus.ADMIN_REJECTED;
        };
    }

    private void updateOrderStatusSafely(Long orderId, boolean forceConfirmed) {
        String currentStatus = orderPort.getRequiredSnapshot(orderId).status();
        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            return;
        }
        if (forceConfirmed) {
            orderPort.setOrderConfirmed(orderId);
        } else {
            orderPort.setOrderDisputed(orderId);
        }
    }

    private void upsertSettlementOnPaid(Long orderId, Users shipper, BigDecimal grossAmount, FeePolicyService.FeeResult fee) {
        // 할인 아직 없으면 0으로 저장
        long levelDiscount = 0L;
        long couponDiscount = 0L;

        // 최종 결제금액(할인 반영). 할인 없으면 gross 그대로.
        long totalPrice = grossAmount.longValue(); // scale이 .00이라 longValue()로 충분

        // feeRate: 0.05 -> 5
        long feeRatePercent = fee.feeRate().multiply(new BigDecimal("100")).longValue();

        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Order orderRef = entityManager.getReference(Order.class, orderId);
                    Users userRef = entityManager.getReference(Users.class, shipper.getUserId());
                    return Settlement.builder()
                            .order(orderRef)
                            .user(userRef)
                            .build();
                });

        settlement.setLevelDiscount(levelDiscount);
        settlement.setCouponDiscount(couponDiscount);
        settlement.setTotalPrice(totalPrice);
        settlement.setFeeRate(feeRatePercent);
        settlement.setStatus("READY");
        settlement.setFeeDate(LocalDateTime.now());

        settlementRepository.save(settlement);
    }

    // externalPay에서는 TOTAL_PRICE를 덮어쓰지 않고, 상태/일시만 갱신
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

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) return null;
        return value.trim();
    }

    private void requireAuthenticated(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private List<TransportPaymentStatus> paidOrConfirmedStatuses() {
        return List.of(
                TransportPaymentStatus.PAID,
                TransportPaymentStatus.CONFIRMED,
                TransportPaymentStatus.ADMIN_FORCE_CONFIRMED
        );
    }
}
