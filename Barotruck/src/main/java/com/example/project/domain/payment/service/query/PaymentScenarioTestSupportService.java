package com.example.project.domain.payment.service.query;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.PaymentScenarioSnapshotResponse;
import com.example.project.domain.payment.dto.paymentResponse.TransportPaymentResponse;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.PaymentGatewayWebhookEventRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider.TOSS;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "payment.test-support.enabled", havingValue = "true")
public class PaymentScenarioTestSupportService {

    private static final int WEBHOOK_FETCH_LIMIT_PER_KEYWORD = 10;
    private static final int WEBHOOK_RESPONSE_LIMIT = 20;

    private final OrderRepository orderRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;
    private final PaymentGatewayWebhookEventRepository paymentGatewayWebhookEventRepository;
    private final AdminPaymentStatusQueryService adminPaymentStatusQueryService;
    private final Environment environment;

    public PaymentScenarioSnapshotResponse getOrderSnapshot(Long orderId) {
        assertTestSupportAllowed();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));

        PaymentGatewayTransaction gatewayTransaction = paymentGatewayTransactionRepository
                .findTopByOrderIdAndProviderOrderByCreatedAtDesc(orderId, TOSS)
                .orElse(null);

        DriverPayoutItemStatusResponse payoutItem = driverPayoutItemRepository.findByOrderId(orderId).isPresent()
                ? adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(orderId)
                : null;

        List<PaymentScenarioSnapshotResponse.WebhookEventSnapshot> webhookEvents = findWebhookSnapshots(
                orderId,
                gatewayTransaction,
                payoutItem
        );

        return PaymentScenarioSnapshotResponse.builder()
                .snapshotAt(LocalDateTime.now())
                .scenario(PaymentScenarioSnapshotResponse.ScenarioRef.builder()
                        .orderId(order.getOrderId())
                        .orderStatus(order.getStatus())
                        .shipperUserId(order.getUser() == null ? null : order.getUser().getUserId())
                        .driverUserId(order.getDriverNo())
                        .pgOrderId(gatewayTransaction == null ? null : gatewayTransaction.getPgOrderId())
                        .paymentKey(gatewayTransaction == null ? null : gatewayTransaction.getPaymentKey())
                        .payoutRef(payoutItem == null ? null : payoutItem.payoutRef())
                        .sellerId(payoutItem == null ? null : payoutItem.sellerId())
                        .build())
                .order(PaymentScenarioSnapshotResponse.OrderSnapshotView.from(order))
                .transportPayment(transportPaymentRepository.findByOrderId(orderId)
                        .map(TransportPaymentResponse::from)
                        .orElse(null))
                .settlement(settlementRepository.findByOrderId(orderId)
                        .map(PaymentScenarioSnapshotResponse.SettlementSnapshot::from)
                        .orElse(null))
                .gatewayTransaction(PaymentScenarioSnapshotResponse.GatewayTransactionSnapshot.from(gatewayTransaction))
                .payoutItem(payoutItem)
                .webhookEvents(webhookEvents)
                .build();
    }

    private List<PaymentScenarioSnapshotResponse.WebhookEventSnapshot> findWebhookSnapshots(
            Long orderId,
            PaymentGatewayTransaction gatewayTransaction,
            DriverPayoutItemStatusResponse payoutItem
    ) {
        LinkedHashMap<Long, PaymentGatewayWebhookEvent> eventsById = new LinkedHashMap<>();

        collectWebhookEvents(eventsById, stringify(orderId));
        collectWebhookEvents(eventsById, gatewayTransaction == null ? null : gatewayTransaction.getPgOrderId());
        collectWebhookEvents(eventsById, gatewayTransaction == null ? null : gatewayTransaction.getPaymentKey());
        collectWebhookEvents(eventsById, payoutItem == null ? null : payoutItem.payoutRef());
        collectWebhookEvents(eventsById, payoutItem == null ? null : payoutItem.sellerId());

        List<PaymentGatewayWebhookEvent> orderedEvents = new ArrayList<>(eventsById.values());
        orderedEvents.sort(Comparator.comparing(PaymentGatewayWebhookEvent::getReceivedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return orderedEvents.stream()
                .limit(WEBHOOK_RESPONSE_LIMIT)
                .map(PaymentScenarioSnapshotResponse.WebhookEventSnapshot::from)
                .toList();
    }

    private void collectWebhookEvents(LinkedHashMap<Long, PaymentGatewayWebhookEvent> sink, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        paymentGatewayWebhookEventRepository
                .findByProviderAndEventTypeContainingIgnoreCaseAndPayloadContainingIgnoreCaseOrderByReceivedAtDesc(
                        TOSS,
                        "",
                        keyword,
                        PageRequest.of(0, WEBHOOK_FETCH_LIMIT_PER_KEYWORD)
                )
                .forEach(event -> sink.putIfAbsent(event.getWebhookId(), event));
    }

    private void assertTestSupportAllowed() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean allowed = activeProfiles.length == 0 || environment.acceptsProfiles(Profiles.of("local", "dev"));
        if (!allowed) {
            throw new IllegalStateException("payment test-support API is available only in local/dev");
        }
    }

    private String stringify(Long value) {
        return value == null ? null : Long.toString(value);
    }
}
