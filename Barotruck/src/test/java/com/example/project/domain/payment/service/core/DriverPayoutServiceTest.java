package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.TransportPaymentPricingSnapshot;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.repository.DriverPayoutBatchRepository;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverPayoutServiceTest {

    @Mock
    private DriverPayoutBatchRepository batchRepository;

    @Mock
    private DriverPayoutItemRepository itemRepository;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private DriverPayoutGatewayClient payoutGatewayClient;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TossPayoutWebhookService tossPayoutWebhookService;

    @Mock
    private PromotionEligibilityService promotionEligibilityService;

    @InjectMocks
    private DriverPayoutService driverPayoutService;

    @Test
    void requestPayoutForOrder_copiesSnapshotAmountsFromTransportPayment() {
        DriverPayoutBatch batch = batch(10L);
        TransportPayment payment = confirmedPayment(30L, 40L);

        when(transportPaymentRepository.findByOrderId(30L)).thenReturn(Optional.of(payment));
        when(itemRepository.findByOrderId(30L)).thenReturn(Optional.empty());
        when(batchRepository.findByBatchDate(LocalDate.now())).thenReturn(Optional.of(batch));
        when(itemRepository.save(any(DriverPayoutItem.class))).thenAnswer(invocation -> {
            DriverPayoutItem item = invocation.getArgument(0);
            if (item.getItemId() == null) {
                ReflectionTestUtils.setField(item, "itemId", 77L);
            }
            return item;
        });
        when(payoutGatewayClient.payout(30L, 40L, new BigDecimal("97500.00"), 10L, 77L))
                .thenReturn(new DriverPayoutGatewayClient.PayoutResult(true, false, "PO-REF-NEW", "REQUESTED", null));
        doAnswer(invocation -> {
            DriverPayoutItem item = invocation.getArgument(0);
            item.markAccepted(invocation.getArgument(1));
            return null;
        }).when(tossPayoutWebhookService).syncPayoutAccepted(any(DriverPayoutItem.class), eq("PO-REF-NEW"), eq("REQUEST"));

        DriverPayoutItem result = driverPayoutService.requestPayoutForOrder(30L);

        assertThat(result.getNetAmount()).isEqualByComparingTo("97500.00");
        assertThat(result.getDriverFeeAmount()).isEqualByComparingTo("2500.00");
        assertThat(result.getShipperChargeAmount()).isEqualByComparingTo("102000.00");
        assertThat(result.getShipperFeeAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.getPlatformGrossRevenue()).isEqualByComparingTo("4500.00");
        assertThat(result.getPlatformNetRevenue()).isEqualByComparingTo("4500.00");
        assertThat(result.getStatus()).isEqualTo(PayoutStatus.REQUESTED);
        assertThat(result.getPayoutRef()).isEqualTo("PO-REF-NEW");
        verify(promotionEligibilityService).applyDriverFirstTransportPromotion(any(DriverPayoutItem.class));
    }

    @Test
    void retryItem_keepsRequestedWhenGatewayOnlyAcceptsPayout() {
        DriverPayoutBatch batch = batch(11L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(20L)
                .batch(batch)
                .orderId(31L)
                .driverUserId(41L)
                .netAmount(new BigDecimal("50000.00"))
                .status(PayoutStatus.FAILED)
                .retryCount(1)
                .build();

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(DriverPayoutItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutGatewayClient.payout(31L, 41L, new BigDecimal("50000.00"), 11L, 20L))
                .thenReturn(new DriverPayoutGatewayClient.PayoutResult(true, false, "PO-REF-1", "REQUESTED", null));
        doAnswer(invocation -> {
            DriverPayoutItem target = invocation.getArgument(0);
            target.markAccepted(invocation.getArgument(1));
            return null;
        }).when(tossPayoutWebhookService).syncPayoutAccepted(any(DriverPayoutItem.class), eq("PO-REF-1"), eq("REQUEST"));

        DriverPayoutItem result = driverPayoutService.retryItem(20L);

        assertThat(result.getStatus()).isEqualTo(PayoutStatus.REQUESTED);
        assertThat(result.getPayoutRef()).isEqualTo("PO-REF-1");
        verify(tossPayoutWebhookService, never()).syncPayoutCompleted(any(), any(), any());
    }

    @Test
    void syncRequestedPayouts_marksCompletedViaWebhookSync() {
        DriverPayoutBatch batch = batch(12L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(21L)
                .batch(batch)
                .orderId(32L)
                .driverUserId(42L)
                .netAmount(new BigDecimal("70000.00"))
                .status(PayoutStatus.REQUESTED)
                .retryCount(0)
                .payoutRef("PO-REF-2")
                .build();

        when(itemRepository.findAllByStatusIn(List.of(PayoutStatus.REQUESTED))).thenReturn(List.of(item));
        when(payoutGatewayClient.getPayoutStatus("PO-REF-2"))
                .thenReturn(new DriverPayoutGatewayClient.PayoutStatusResult(true, true, false, "COMPLETED", null));
        doAnswer(invocation -> {
            DriverPayoutItem target = invocation.getArgument(0);
            target.markCompleted(invocation.getArgument(1));
            return null;
        }).when(tossPayoutWebhookService).syncPayoutCompleted(any(DriverPayoutItem.class), eq("PO-REF-2"), eq("POLLING"));

        driverPayoutService.syncRequestedPayouts();

        assertThat(item.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
        assertThat(item.getCompletedAt()).isNotNull();
        verify(tossPayoutWebhookService).syncPayoutCompleted(item, "PO-REF-2", "POLLING");
    }

    @Test
    void syncRequestedPayouts_marksFailedWithoutIncrementingRetryCountOnStatusPollFailure() {
        DriverPayoutBatch batch = batch(13L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(22L)
                .batch(batch)
                .orderId(33L)
                .driverUserId(43L)
                .netAmount(new BigDecimal("80000.00"))
                .status(PayoutStatus.REQUESTED)
                .retryCount(2)
                .payoutRef("PO-REF-3")
                .build();

        when(itemRepository.findAllByStatusIn(List.of(PayoutStatus.REQUESTED))).thenReturn(List.of(item));
        when(payoutGatewayClient.getPayoutStatus("PO-REF-3"))
                .thenReturn(new DriverPayoutGatewayClient.PayoutStatusResult(true, false, true, "FAILED", "bank rejected"));
        doAnswer(invocation -> {
            DriverPayoutItem target = invocation.getArgument(0);
            target.markFailed(invocation.getArgument(1), false);
            return null;
        }).when(tossPayoutWebhookService).syncPayoutFailed(any(DriverPayoutItem.class), eq("bank rejected"), eq(false), eq("POLLING"));

        driverPayoutService.syncRequestedPayouts();

        assertThat(item.getStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(item.getRetryCount()).isEqualTo(2);
        assertThat(item.getFailureReason()).isEqualTo("bank rejected");
    }

    private TransportPayment confirmedPayment(Long orderId, Long driverUserId) {
        TransportPayment payment = TransportPayment.ready(
                orderId,
                20L,
                driverUserId,
                new BigDecimal("102000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                new BigDecimal("97500.00"),
                PaymentMethod.TRANSFER
        );
        payment.applyPricingSnapshot(new TransportPaymentPricingSnapshot(
                new BigDecimal("102000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                false,
                new BigDecimal("102000.00"),
                new BigDecimal("0.0250"),
                new BigDecimal("2500.00"),
                false,
                new BigDecimal("97500.00"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("4500.00"),
                1L,
                LocalDateTime.of(2026, 3, 1, 0, 0)
        ));
        payment.markPaid("PG-OK", LocalDateTime.of(2026, 3, 10, 9, 0));
        payment.confirm(LocalDateTime.of(2026, 3, 10, 10, 0));
        payment.setPgTid("PG-OK");
        payment.updateStatus(TransportPaymentStatus.CONFIRMED);
        return payment;
    }

    private DriverPayoutBatch batch(Long batchId) {
        return DriverPayoutBatch.builder()
                .batchId(batchId)
                .batchDate(LocalDate.of(2026, 3, 10))
                .status(PayoutStatus.REQUESTED)
                .build();
    }
}
