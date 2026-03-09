package com.example.project.domain.payment.service.core;

import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentRequest.CreatePaymentDisputeRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdatePaymentDisputeStatusRequest;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentDisputeService {

    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentDisputeRepository paymentDisputeRepository;
    private final SettlementRepository settlementRepository;
    private final OrderPort orderPort;
    private final NotificationService notificationService;
    private final UsersRepository usersRepository;

    @Transactional
    public PaymentDispute createDispute(Users currentUser, Long orderId, CreatePaymentDisputeRequest request) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.DRIVER) {
            throw new IllegalStateException("only driver/admin can create dispute");
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
            s.setStatus(mapDisputeStatusToSettlementStatus(nextStatus));
            settlementRepository.save(s);
        });

        PaymentDispute saved = paymentDisputeRepository.save(dispute);
        notifyDisputeStatusUpdated(payment, nextStatus, orderId);
        return saved;
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
        notifyDisputeCreated(currentUser, payment, requesterUserId, orderId, reasonCode);
        return saved;
    }

    private void applyDisputedState(Long orderId, TransportPayment payment) {
        payment.dispute();
        transportPaymentRepository.save(payment);
        updateOrderStatusSafely(orderId, false);

        settlementRepository.findByOrderId(orderId).ifPresent(s -> {
            s.setStatus(SettlementStatus.WAIT);
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

    private SettlementStatus mapDisputeStatusToSettlementStatus(PaymentDisputeStatus status) {
        return switch (status) {
            case PENDING -> SettlementStatus.WAIT;
            case ADMIN_HOLD -> SettlementStatus.WAIT;
            case ADMIN_FORCE_CONFIRMED -> SettlementStatus.COMPLETED;
            case ADMIN_REJECTED -> SettlementStatus.WAIT;
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

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private void requireAuthenticated(Users currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private void notifyDisputeCreated(
            Users currentUser,
            TransportPayment payment,
            Long requesterUserId,
            Long orderId,
            PaymentDisputeReason reasonCode
    ) {
        Long opponentUserId = null;
        if (requesterUserId != null && requesterUserId.equals(payment.getDriverUserId())) {
            opponentUserId = payment.getShipperUserId();
        } else if (requesterUserId != null && requesterUserId.equals(payment.getShipperUserId())) {
            opponentUserId = payment.getDriverUserId();
        }

        sendPaymentNotificationSafely(
                opponentUserId,
                "이의제기 등록",
                String.format("결제 이의제기가 등록되었습니다. 사유: %s", reasonCode.name()),
                orderId
        );

        usersRepository.findAllByRole(Role.ADMIN, Sort.by(Sort.Direction.ASC, "userId"))
                .stream()
                .filter(admin -> currentUser == null || !admin.getUserId().equals(currentUser.getUserId()))
                .forEach(admin -> sendPaymentNotificationSafely(
                        admin.getUserId(),
                        "이의제기 접수",
                        String.format("주문 %d 에 결제 이의제기가 접수되었습니다.", orderId),
                        orderId
                ));
    }

    private void notifyDisputeStatusUpdated(
            TransportPayment payment,
            PaymentDisputeStatus status,
            Long orderId
    ) {
        String body = switch (status) {
            case ADMIN_HOLD -> "이의제기가 검토 중입니다.";
            case ADMIN_FORCE_CONFIRMED -> "이의제기 처리 결과 정산이 확정되었습니다.";
            case ADMIN_REJECTED -> "이의제기가 반려되었습니다.";
            case PENDING -> "이의제기가 접수되었습니다.";
        };

        sendPaymentNotificationSafely(
                payment.getShipperUserId(),
                "이의제기 처리 결과",
                body,
                orderId
        );
        sendPaymentNotificationSafely(
                payment.getDriverUserId(),
                "이의제기 처리 결과",
                body,
                orderId
        );
    }

    private void sendPaymentNotificationSafely(Long recipientUserId, String title, String body, Long targetId) {
        if (recipientUserId == null) {
            return;
        }
        try {
            notificationService.sendNotification(recipientUserId, "PAYMENT", title, body, targetId);
        } catch (Exception e) {
            System.out.println("분쟁 알림 발송 중 예외 발생: " + e.getMessage());
        }
    }
}


