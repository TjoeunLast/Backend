package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.PaymentGatewayWebhookEvent;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.GatewayTxStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PaymentScenarioSnapshotResponse(
        LocalDateTime snapshotAt,
        ScenarioRef scenario,
        OrderSnapshotView order,
        TransportPaymentResponse transportPayment,
        SettlementSnapshot settlement,
        GatewayTransactionSnapshot gatewayTransaction,
        DriverPayoutItemStatusResponse payoutItem,
        List<WebhookEventSnapshot> webhookEvents
) {

    @Builder
    public record ScenarioRef(
            Long orderId,
            String orderStatus,
            Long shipperUserId,
            Long driverUserId,
            String pgOrderId,
            String paymentKey,
            String payoutRef,
            String sellerId
    ) {
    }

    @Builder
    public record OrderSnapshotView(
            Long orderId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long shipperUserId,
            String shipperNickname,
            Long driverUserId,
            BigDecimal distance,
            BigDecimal duration,
            String startAddr,
            String startPlace,
            String startType,
            String startSchedule,
            String puProvince,
            BigDecimal startLat,
            BigDecimal startLng,
            String endAddr,
            String endPlace,
            String endType,
            String endSchedule,
            String doProvince,
            String cargoContent,
            String loadMethod,
            String workType,
            BigDecimal tonnage,
            String reqCarType,
            String reqTonnage,
            String driveMode,
            Long loadWeight,
            Long basePrice,
            Long laborFee,
            Long packagingPrice,
            Long insuranceFee,
            String payMethod,
            boolean instant,
            String memo
    ) {
        public static OrderSnapshotView from(Order order) {
            if (order == null) {
                return null;
            }
            OrderSnapshot snapshot = order.getSnapshot();
            return OrderSnapshotView.builder()
                    .orderId(order.getOrderId())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdated())
                    .shipperUserId(order.getUser() == null ? null : order.getUser().getUserId())
                    .shipperNickname(order.getUser() == null ? null : order.getUser().getNickname())
                    .driverUserId(order.getDriverNo())
                    .distance(order.getDistance())
                    .duration(order.getDuration())
                    .startAddr(snapshot == null ? null : snapshot.getStartAddr())
                    .startPlace(snapshot == null ? null : snapshot.getStartPlace())
                    .startType(snapshot == null ? null : snapshot.getStartType())
                    .startSchedule(snapshot == null ? null : snapshot.getStartSchedule())
                    .puProvince(snapshot == null ? null : snapshot.getPuProvince())
                    .startLat(snapshot == null ? null : snapshot.getStartLat())
                    .startLng(snapshot == null ? null : snapshot.getStartLng())
                    .endAddr(snapshot == null ? null : snapshot.getEndAddr())
                    .endPlace(snapshot == null ? null : snapshot.getEndPlace())
                    .endType(snapshot == null ? null : snapshot.getEndType())
                    .endSchedule(snapshot == null ? null : snapshot.getEndSchedule())
                    .doProvince(snapshot == null ? null : snapshot.getDoProvince())
                    .cargoContent(snapshot == null ? null : snapshot.getCargoContent())
                    .loadMethod(snapshot == null ? null : snapshot.getLoadMethod())
                    .workType(snapshot == null ? null : snapshot.getWorkType())
                    .tonnage(snapshot == null ? null : snapshot.getTonnage())
                    .reqCarType(snapshot == null ? null : snapshot.getReqCarType())
                    .reqTonnage(snapshot == null ? null : snapshot.getReqTonnage())
                    .driveMode(snapshot == null ? null : snapshot.getDriveMode())
                    .loadWeight(snapshot == null ? null : snapshot.getLoadWeight())
                    .basePrice(snapshot == null ? null : snapshot.getBasePrice())
                    .laborFee(snapshot == null ? null : snapshot.getLaborFee())
                    .packagingPrice(snapshot == null ? null : snapshot.getPackagingPrice())
                    .insuranceFee(snapshot == null ? null : snapshot.getInsuranceFee())
                    .payMethod(snapshot == null ? null : snapshot.getPayMethod())
                    .instant(snapshot != null && snapshot.isInstant())
                    .memo(snapshot == null ? null : snapshot.getMemo())
                    .build();
        }
    }

    @Builder
    public record SettlementSnapshot(
            Long settlementId,
            SettlementStatus status,
            Long totalPrice,
            Long feeRate,
            Long userId,
            LocalDateTime feeDate,
            LocalDateTime feeCompleteDate,
            String field5,
            String field6
    ) {
        public static SettlementSnapshot from(Settlement settlement) {
            if (settlement == null) {
                return null;
            }
            return SettlementSnapshot.builder()
                    .settlementId(settlement.getId())
                    .status(settlement.getStatus())
                    .totalPrice(settlement.getTotalPrice())
                    .feeRate(settlement.getFeeRate())
                    .userId(settlement.getUser() == null ? null : settlement.getUser().getUserId())
                    .feeDate(settlement.getFeeDate())
                    .feeCompleteDate(settlement.getFeeCompleteDate())
                    .field5(settlement.getField5())
                    .field6(settlement.getField6())
                    .build();
        }
    }

    @Builder
    public record GatewayTransactionSnapshot(
            Long txId,
            Long orderId,
            Long shipperUserId,
            PaymentProvider provider,
            PaymentMethod method,
            PayChannel payChannel,
            String pgOrderId,
            String paymentKey,
            String transactionId,
            BigDecimal amount,
            GatewayTxStatus status,
            String gatewayStatus,
            String idempotencyKey,
            String successUrl,
            String failUrl,
            LocalDateTime expiresAt,
            LocalDateTime approvedAt,
            String cancelReason,
            BigDecimal canceledAmount,
            LocalDateTime canceledAt,
            String cancelTransactionId,
            String failCode,
            String failMessage,
            Integer retryCount,
            LocalDateTime lastRetryAt,
            LocalDateTime nextRetryAt,
            String rawPayload,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static GatewayTransactionSnapshot from(PaymentGatewayTransaction transaction) {
            if (transaction == null) {
                return null;
            }
            return GatewayTransactionSnapshot.builder()
                    .txId(transaction.getTxId())
                    .orderId(transaction.getOrderId())
                    .shipperUserId(transaction.getShipperUserId())
                    .provider(transaction.getProvider())
                    .method(transaction.getMethod())
                    .payChannel(transaction.getPayChannel())
                    .pgOrderId(transaction.getPgOrderId())
                    .paymentKey(transaction.getPaymentKey())
                    .transactionId(transaction.getTransactionId())
                    .amount(transaction.getAmount())
                    .status(transaction.getStatus())
                    .gatewayStatus(transaction.getGatewayStatus())
                    .idempotencyKey(transaction.getIdempotencyKey())
                    .successUrl(transaction.getSuccessUrl())
                    .failUrl(transaction.getFailUrl())
                    .expiresAt(transaction.getExpiresAt())
                    .approvedAt(transaction.getApprovedAt())
                    .cancelReason(transaction.getCancelReason())
                    .canceledAmount(transaction.getCanceledAmount())
                    .canceledAt(transaction.getCanceledAt())
                    .cancelTransactionId(transaction.getCancelTransactionId())
                    .failCode(transaction.getFailCode())
                    .failMessage(transaction.getFailMessage())
                    .retryCount(transaction.getRetryCount())
                    .lastRetryAt(transaction.getLastRetryAt())
                    .nextRetryAt(transaction.getNextRetryAt())
                    .rawPayload(transaction.getRawPayload())
                    .createdAt(transaction.getCreatedAt())
                    .updatedAt(transaction.getUpdatedAt())
                    .build();
        }
    }

    @Builder
    public record WebhookEventSnapshot(
            Long webhookId,
            String externalEventId,
            String eventType,
            String processResult,
            LocalDateTime receivedAt,
            LocalDateTime processedAt,
            String payload
    ) {
        public static WebhookEventSnapshot from(PaymentGatewayWebhookEvent event) {
            if (event == null) {
                return null;
            }
            return WebhookEventSnapshot.builder()
                    .webhookId(event.getWebhookId())
                    .externalEventId(event.getExternalEventId())
                    .eventType(event.getEventType())
                    .processResult(event.getProcessResult())
                    .receivedAt(event.getReceivedAt())
                    .processedAt(event.getProcessedAt())
                    .payload(event.getPayload())
                    .build();
        }
    }
}
