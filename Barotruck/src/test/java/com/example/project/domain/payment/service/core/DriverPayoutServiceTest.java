package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverPayoutServiceTest {

    @Mock
    private DriverPayoutItemRepository itemRepository;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private DriverPayoutGatewayClient payoutGatewayClient;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DriverPayoutService driverPayoutService;

    @Test
    void retryItem_keepsRequestedWhenGatewayOnlyAcceptsPayout() {
        DriverPayoutBatch batch = batch(10L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(20L)
                .batch(batch)
                .orderId(30L)
                .driverUserId(40L)
                .netAmount(new BigDecimal("50000"))
                .status(PayoutStatus.FAILED)
                .retryCount(1)
                .build();

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(DriverPayoutItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutGatewayClient.payout(30L, 40L, new BigDecimal("50000"), 10L, 20L))
                .thenReturn(new DriverPayoutGatewayClient.PayoutResult(true, false, "PO-REF-1", "REQUESTED", null));
        when(itemRepository.countByBatch_BatchId(10L)).thenReturn(1L);
        when(itemRepository.countByBatch_BatchIdAndStatus(10L, PayoutStatus.REQUESTED)).thenReturn(1L);
        when(itemRepository.countByBatch_BatchIdAndStatus(10L, PayoutStatus.FAILED)).thenReturn(0L);

        DriverPayoutItem result = driverPayoutService.retryItem(20L);

        assertThat(result.getStatus()).isEqualTo(PayoutStatus.REQUESTED);
        assertThat(result.getPayoutRef()).isEqualTo("PO-REF-1");
        verify(settlementRepository, never()).findByOrderId(30L);
    }

    @Test
    void syncRequestedPayouts_marksSettlementCompletedWhenGatewayCompletes() {
        DriverPayoutBatch batch = batch(11L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(21L)
                .batch(batch)
                .orderId(31L)
                .driverUserId(41L)
                .netAmount(new BigDecimal("70000"))
                .status(PayoutStatus.REQUESTED)
                .retryCount(0)
                .payoutRef("PO-REF-2")
                .build();
        Settlement settlement = Settlement.builder()
                .id(100L)
                .status(SettlementStatus.READY)
                .build();

        when(itemRepository.findAllByStatusIn(List.of(PayoutStatus.REQUESTED))).thenReturn(List.of(item));
        when(payoutGatewayClient.getPayoutStatus("PO-REF-2"))
                .thenReturn(new DriverPayoutGatewayClient.PayoutStatusResult(true, true, false, "COMPLETED", null));
        when(itemRepository.save(any(DriverPayoutItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.findByOrderId(31L)).thenReturn(Optional.of(settlement));
        when(itemRepository.countByBatch_BatchId(11L)).thenReturn(1L);
        when(itemRepository.countByBatch_BatchIdAndStatus(11L, PayoutStatus.REQUESTED)).thenReturn(0L);
        when(itemRepository.countByBatch_BatchIdAndStatus(11L, PayoutStatus.FAILED)).thenReturn(0L);

        driverPayoutService.syncRequestedPayouts();

        assertThat(item.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(settlement.getFeeCompleteDate()).isNotNull();
        verify(settlementRepository).save(settlement);
    }

    @Test
    void syncRequestedPayouts_marksFailedWithoutIncrementingRetryCountOnStatusPollFailure() {
        DriverPayoutBatch batch = batch(12L);
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(22L)
                .batch(batch)
                .orderId(32L)
                .driverUserId(42L)
                .netAmount(new BigDecimal("80000"))
                .status(PayoutStatus.REQUESTED)
                .retryCount(2)
                .payoutRef("PO-REF-3")
                .build();

        when(itemRepository.findAllByStatusIn(List.of(PayoutStatus.REQUESTED))).thenReturn(List.of(item));
        when(payoutGatewayClient.getPayoutStatus("PO-REF-3"))
                .thenReturn(new DriverPayoutGatewayClient.PayoutStatusResult(true, false, true, "FAILED", "bank rejected"));
        when(itemRepository.save(any(DriverPayoutItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.countByBatch_BatchId(12L)).thenReturn(1L);
        when(itemRepository.countByBatch_BatchIdAndStatus(12L, PayoutStatus.REQUESTED)).thenReturn(0L);
        when(itemRepository.countByBatch_BatchIdAndStatus(12L, PayoutStatus.FAILED)).thenReturn(1L);

        driverPayoutService.syncRequestedPayouts();

        assertThat(item.getStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(item.getRetryCount()).isEqualTo(2);
        assertThat(item.getFailureReason()).isEqualTo("bank rejected");
        verify(settlementRepository, never()).findByOrderId(32L);
    }

    private DriverPayoutBatch batch(Long batchId) {
        return DriverPayoutBatch.builder()
                .batchId(batchId)
                .batchDate(LocalDate.of(2026, 3, 10))
                .status(PayoutStatus.REQUESTED)
                .build();
    }
}
