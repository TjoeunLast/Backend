package com.example.project.global.toss.service;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.dto.paymentRequest.TossConfirmRequest;
import com.example.project.domain.payment.dto.paymentRequest.TossPrepareRequest;
import com.example.project.domain.payment.dto.paymentResponse.TossPrepareResponse;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.service.core.PaymentLifecycleService;
import com.example.project.global.toss.client.TossPaymentClient;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final TossPaymentClient tossPaymentClient;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final PaymentLifecycleService paymentLifecycleService;
    private final OrderPort orderPort;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional
    public TossPrepareResponse prepare(Users currentUser, Long orderId, TossPrepareRequest request) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can prepare toss payment");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        OrderPort.OrderSnapshot snap = orderPort.getRequiredSnapshot(orderId);
        if (snap.shipperUserId() == null || !snap.shipperUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("shipper can prepare only own order");
        }

        BigDecimal amount = snap.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("invalid payment amount");
        }

        PaymentMethod method = resolveTossMethod(request.getMethod(), request.getPayChannel());
        PayChannel payChannel = resolvePayChannel(method, request.getPayChannel());
        String pgOrderId = createPgOrderId(orderId);
        String successUrl = defaultIfBlank(request.getSuccessUrl(), "/payments/success?orderId=" + orderId);
        String failUrl = defaultIfBlank(request.getFailUrl(), "/payments/fail?orderId=" + orderId);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        PaymentGatewayTransaction tx = PaymentGatewayTransaction.prepare(
                orderId,
                snap.shipperUserId(),
                PaymentProvider.TOSS,
                method,
                payChannel,
                pgOrderId,
                amount,
                UUID.randomUUID().toString(),
                successUrl,
                failUrl,
                expiresAt
        );
        paymentGatewayTransactionRepository.save(tx);

        return TossPrepareResponse.builder()
                .provider(PaymentProvider.TOSS)
                .pgOrderId(pgOrderId)
                .amount(amount)
                .successUrl(successUrl)
                .failUrl(failUrl)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public TransportPayment confirm(Users currentUser, Long orderId, TossConfirmRequest request) {
        requireAuthenticated(currentUser);
        if (currentUser.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("only shipper can confirm toss payment");
        }
        if (request == null
                || isBlank(request.getPaymentKey())
                || isBlank(request.getPgOrderId())
                || request.getAmount() == null) {
            throw new IllegalArgumentException("paymentKey, pgOrderId, amount are required");
        }

        PaymentGatewayTransaction tx = paymentGatewayTransactionRepository
                .findByProviderAndPgOrderId(PaymentProvider.TOSS, request.getPgOrderId())
                .orElseThrow(() -> new IllegalArgumentException("prepared transaction not found"));

        if (tx.getStatus() != GatewayTxStatus.CONFIRMED && tx.isExpired(LocalDateTime.now())) {
            tx.markExpired();
            paymentGatewayTransactionRepository.save(tx);
            throw new IllegalStateException("prepared transaction expired");
        }

        if (!tx.getOrderId().equals(orderId)) {
            throw new IllegalStateException("transaction/order mismatch");
        }
        if (!tx.getShipperUserId().equals(currentUser.getUserId())) {
            throw new IllegalStateException("shipper can confirm only own order");
        }
        if (tx.getAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalStateException("amount mismatch");
        }

        paymentGatewayTransactionRepository
                .findByProviderAndPaymentKey(PaymentProvider.TOSS, request.getPaymentKey())
                .ifPresent(existing -> {
                    if (!existing.getTxId().equals(tx.getTxId())) {
                        throw new IllegalStateException("paymentKey already used");
                    }
                });

        if (tx.getStatus() == GatewayTxStatus.CANCELED) {
            throw new IllegalStateException("transaction already canceled");
        }

        tx.bindPaymentKey(request.getPaymentKey());

        if (tx.getStatus() == GatewayTxStatus.CONFIRMED) {
            return paymentLifecycleService.applyPaidFromGatewayTx(tx);
        }

        TossPaymentClient.ConfirmResult confirmResult =
                tossPaymentClient.confirm(request.getPaymentKey(), request.getPgOrderId(), request.getAmount());

        if (!confirmResult.success()) {
            tx.markFailed(
                    confirmResult.failCode(),
                    confirmResult.failMessage(),
                    confirmResult.rawPayload(),
                    isRetryableFailure(confirmResult.failCode(), confirmResult.failMessage())
            );
            paymentGatewayTransactionRepository.save(tx);
            throw new IllegalStateException("toss confirm failed: " + defaultIfBlank(confirmResult.failMessage(), "unknown"));
        }

        tx.markConfirmed(request.getPaymentKey(), confirmResult.transactionId(), confirmResult.rawPayload());
        paymentGatewayTransactionRepository.save(tx);
        return paymentLifecycleService.applyPaidFromGatewayTx(tx);
    }

    @Transactional
    public void handleWebhook(String eventId, String payload) {
        String externalEventId = defaultIfBlank(eventId, "NO-ID-" + UUID.randomUUID());
        String safePayload = payload == null ? "{}" : payload;

        PaymentGatewayWebhookEvent existing = findWebhookEvent(externalEventId);
        if (existing != null) {
            return;
        }

        String eventType = "UNKNOWN";
        try {
            JsonNode node = objectMapper.readTree(safePayload);
            eventType = firstNonBlank(
                    readText(node, "eventType"),
                    readText(node, "type"),
                    readText(node, "event"),
                    readText(node, "status"),
                    "UNKNOWN"
            );
        } catch (Exception ignored) {
        }

        PaymentGatewayWebhookEvent webhookEvent = PaymentGatewayWebhookEvent.builder()
                .provider(PaymentProvider.TOSS)
                .externalEventId(externalEventId)
                .eventType(eventType)
                .payload(safePayload)
                .receivedAt(LocalDateTime.now())
                .build();
        entityManager.persist(webhookEvent);

        try {
            JsonNode node = objectMapper.readTree(safePayload);
            String paymentKey = readText(node, "paymentKey");
            String pgOrderId = firstNonBlank(
                    readText(node, "orderId"),
                    readText(node, "pgOrderId"),
                    readText(node, "merchantOrderId")
            );
            String status = firstNonBlank(readText(node, "status"), webhookEvent.getEventType()).toUpperCase(Locale.ROOT);

            PaymentGatewayTransaction tx = findTransactionFromWebhook(paymentKey, pgOrderId);
            if (tx == null) {
                webhookEvent.markProcessed("NO_TX");
                return;
            }

            if (isCanceledStatus(status)) {
                tx.markCanceled(safePayload);
                paymentGatewayTransactionRepository.save(tx);
                webhookEvent.markProcessed("CANCELED");
            } else if (isFailedStatus(status)) {
                tx.markFailed(
                        readText(node, "code"),
                        firstNonBlank(readText(node, "message"), readText(node, "failReason")),
                        safePayload,
                        false
                );
                paymentGatewayTransactionRepository.save(tx);
                webhookEvent.markProcessed("FAILED");
            } else if (isConfirmedStatus(status)) {
                if (tx.getStatus() != GatewayTxStatus.CONFIRMED) {
                    String resolvedPaymentKey = defaultIfBlank(paymentKey, tx.getPaymentKey());
                    String resolvedTxId = defaultIfBlank(paymentKey, tx.getTransactionId());
                    tx.markConfirmed(resolvedPaymentKey, resolvedTxId, safePayload);
                    paymentGatewayTransactionRepository.save(tx);
                }
                paymentLifecycleService.applyPaidFromGatewayTx(tx);
                webhookEvent.markProcessed("CONFIRMED");
            } else {
                webhookEvent.markProcessed("IGNORED");
            }
        } catch (Exception e) {
            webhookEvent.markProcessed("ERROR:" + trimLength(defaultIfBlank(e.getMessage(), "unknown"), 150));
        }
    }

    private PaymentGatewayTransaction findTransactionFromWebhook(String paymentKey, String pgOrderId) {
        if (!isBlank(paymentKey)) {
            PaymentGatewayTransaction byPaymentKey = paymentGatewayTransactionRepository
                    .findByProviderAndPaymentKey(PaymentProvider.TOSS, paymentKey)
                    .orElse(null);
            if (byPaymentKey != null) {
                return byPaymentKey;
            }
        }
        if (!isBlank(pgOrderId)) {
            return paymentGatewayTransactionRepository
                    .findByProviderAndPgOrderId(PaymentProvider.TOSS, pgOrderId)
                    .orElse(null);
        }
        return null;
    }

    private PaymentGatewayWebhookEvent findWebhookEvent(String externalEventId) {
        var result = entityManager.createQuery(
                        "select e from PaymentGatewayWebhookEvent e where e.provider = :provider and e.externalEventId = :externalEventId",
                        PaymentGatewayWebhookEvent.class
                )
                .setParameter("provider", PaymentProvider.TOSS)
                .setParameter("externalEventId", externalEventId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
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
            throw new IllegalStateException("toss does not support CASH");
        }
        return method;
    }

    private PayChannel resolvePayChannel(PaymentMethod method, PayChannel requestedChannel) {
        if (requestedChannel != null) {
            return requestedChannel;
        }
        if (method == PaymentMethod.CARD) {
            return PayChannel.CARD;
        }
        return PayChannel.TRANSFER;
    }

    private String createPgOrderId(Long orderId) {
        return "TOSS-" + orderId + "-" + System.currentTimeMillis();
    }

    private boolean isConfirmedStatus(String status) {
        String upper = status.toUpperCase(Locale.ROOT);
        return upper.contains("DONE") || upper.contains("CONFIRMED") || upper.contains("PAID") || upper.contains("SUCCESS");
    }

    private boolean isFailedStatus(String status) {
        String upper = status.toUpperCase(Locale.ROOT);
        return upper.contains("FAIL") || upper.contains("ABORT") || upper.contains("EXPIRED");
    }

    private boolean isCanceledStatus(String status) {
        return status.toUpperCase(Locale.ROOT).contains("CANCEL");
    }

    private boolean isRetryableFailure(String failCode, String failMessage) {
        String code = defaultIfBlank(failCode, "").toUpperCase(Locale.ROOT);
        String message = defaultIfBlank(failMessage, "").toUpperCase(Locale.ROOT);

        if (code.contains("INVALID") || code.contains("UNAUTHORIZED")) {
            return false;
        }
        if (code.contains("AMOUNT_MISMATCH") || message.contains("AMOUNT")) {
            return false;
        }
        if (code.contains("EXPIRED")) {
            return false;
        }
        return true;
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null ? null : text.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = normalize(value);
        return isBlank(normalized) ? defaultValue : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
}



