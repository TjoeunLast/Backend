package com.example.project.domain.payment.service.core;

import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.payment.domain.PaymentGatewayTransaction;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentTiming;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleServiceTest {

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private OrderPort orderPort;

    @Mock
    private MarketplaceFeeCalculationService marketplaceFeeCalculationService;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @Mock
    private DriverPayoutService driverPayoutService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentLifecycleService paymentLifecycleService;

    @Test
    void markPaid_persistsDetailedSnapshotsToPaymentAndSettlement() {
        Long orderId = 100L;
        Long shipperUserId = 200L;
        Long driverUserId = 300L;
        LocalDateTime policyUpdatedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime paidAt = LocalDateTime.of(2026, 3, 11, 14, 30);

        Users shipper = Users.builder()
                .userId(shipperUserId)
                .role(Role.SHIPPER)
                .email("shipper@test.com")
                .build();
        TransportPayment payment = TransportPayment.ready(
                orderId,
                shipperUserId,
                driverUserId,
                new BigDecimal("100000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                new BigDecimal("96500.00"),
                PaymentMethod.TRANSFER,
                PaymentTiming.PREPAID
        );

        AtomicReference<Settlement> settlementRef = new AtomicReference<>();
        when(orderPort.getRequiredSnapshot(orderId)).thenReturn(
                new OrderPort.OrderSnapshot(orderId, shipperUserId, driverUserId, new BigDecimal("100000.00"), "COMPLETED", "TRANSFER")
        );
        when(transportPaymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(marketplaceFeeCalculationService.calculate(any())).thenReturn(
                FeeBreakdownPreviewResponse.builder()
                        .baseAmount(new BigDecimal("100000.00"))
                        .shipperAppliedLevel(1L)
                        .driverAppliedLevel(0L)
                        .shipperFeeRate(new BigDecimal("0.0200"))
                        .driverFeeRate(new BigDecimal("0.0150"))
                        .shipperFeeAmount(new BigDecimal("2000.00"))
                        .driverFeeAmount(new BigDecimal("1500.00"))
                        .shipperPromoApplied(true)
                        .driverPromoApplied(false)
                        .shipperMinFeeApplied(false)
                        .driverMinFeeApplied(false)
                        .shipperChargeAmount(new BigDecimal("100000.00"))
                        .driverPayoutAmount(new BigDecimal("96500.00"))
                        .tossFeeRate(new BigDecimal("0.0000"))
                        .tossFeeAmount(new BigDecimal("0.00"))
                        .platformGrossRevenue(new BigDecimal("3500.00"))
                        .platformNetRevenue(new BigDecimal("3500.00"))
                        .policyUpdatedAt(policyUpdatedAt)
                        .build()
        );
        when(transportPaymentRepository.save(any(TransportPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.findByOrderId(orderId)).thenAnswer(invocation -> Optional.ofNullable(settlementRef.get()));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement saved = invocation.getArgument(0);
            settlementRef.set(saved);
            return saved;
        });
        when(entityManager.getReference(Order.class, orderId)).thenReturn(org.mockito.Mockito.mock(Order.class));
        when(entityManager.getReference(Users.class, shipperUserId)).thenReturn(org.mockito.Mockito.mock(Users.class));

        TransportPayment saved = paymentLifecycleService.markPaid(
                shipper,
                orderId,
                PaymentMethod.TRANSFER,
                PaymentTiming.PREPAID,
                "proof-url",
                paidAt
        );

        assertThat(saved.getStatus()).isEqualTo(TransportPaymentStatus.PAID);
        assertThat(saved.getAmount()).isEqualByComparingTo("100000.00");
        assertThat(saved.getBaseAmountSnapshot()).isEqualByComparingTo("100000.00");
        assertThat(saved.getShipperFeeAmountSnapshot()).isEqualByComparingTo("2000.00");
        assertThat(saved.getDriverFeeAmountSnapshot()).isEqualByComparingTo("1500.00");
        assertThat(saved.getDriverPayoutAmountSnapshot()).isEqualByComparingTo("96500.00");
        assertThat(saved.getTossFeeAmountSnapshot()).isEqualByComparingTo("0.00");
        assertThat(saved.getPlatformGrossRevenueSnapshot()).isEqualByComparingTo("3500.00");
        assertThat(saved.getPlatformNetRevenueSnapshot()).isEqualByComparingTo("3500.00");
        assertThat(saved.getShipperPromoApplied()).isTrue();
        assertThat(saved.getDriverPromoApplied()).isFalse();
        assertThat(saved.getFeePolicyIdSnapshot()).isNull();
        assertThat(saved.getFeePolicyAppliedAtSnapshot()).isEqualTo(policyUpdatedAt);

        Settlement settlement = settlementRef.get();
        assertThat(settlement).isNotNull();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);
        assertThat(settlement.getFeeDate()).isEqualTo(paidAt);
        assertThat(settlement.getBaseAmountSnapshot()).isEqualTo(100000L);
        assertThat(settlement.getShipperChargeAmountSnapshot()).isEqualTo(100000L);
        assertThat(settlement.getDriverPayoutAmountSnapshot()).isEqualTo(96500L);
        assertThat(settlement.getPlatformNetRevenueSnapshot()).isEqualTo(3500L);
        assertThat(settlement.getFeePolicyAppliedAtSnapshot()).isEqualTo(policyUpdatedAt);
        verify(orderPort).setOrderPaid(orderId);
    }

    @Test
    void applyPaidFromGatewayTx_autoConfirmCopiesTossSnapshotsIntoCompletedSettlement() {
        Long orderId = 101L;
        Long shipperUserId = 201L;
        Long driverUserId = 301L;
        LocalDateTime policyUpdatedAt = LocalDateTime.of(2026, 3, 2, 10, 0);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 3, 11, 15, 0);

        TransportPayment payment = TransportPayment.ready(
                orderId,
                shipperUserId,
                driverUserId,
                new BigDecimal("100000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                new BigDecimal("85500.00"),
                PaymentMethod.CARD,
                PaymentTiming.POSTPAID
        );
        PaymentGatewayTransaction tx = PaymentGatewayTransaction.builder()
                .orderId(orderId)
                .shipperUserId(shipperUserId)
                .pgOrderId("TOSS-101")
                .paymentKey("payment-key")
                .transactionId("transaction-id")
                .amount(new BigDecimal("100000.00"))
                .approvedAt(approvedAt)
                .build();

        AtomicReference<Settlement> settlementRef = new AtomicReference<>();
        when(orderPort.getRequiredSnapshot(orderId)).thenReturn(
                new OrderPort.OrderSnapshot(orderId, shipperUserId, driverUserId, new BigDecimal("100000.00"), "COMPLETED", "CARD")
        );
        when(transportPaymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(marketplaceFeeCalculationService.calculate(any())).thenReturn(
                FeeBreakdownPreviewResponse.builder()
                        .baseAmount(new BigDecimal("100000.00"))
                        .shipperAppliedLevel(0L)
                        .driverAppliedLevel(0L)
                        .shipperFeeRate(new BigDecimal("0.0200"))
                        .driverFeeRate(new BigDecimal("0.0250"))
                        .shipperFeeAmount(new BigDecimal("2000.00"))
                        .driverFeeAmount(new BigDecimal("2500.00"))
                        .shipperPromoApplied(false)
                        .driverPromoApplied(false)
                        .shipperMinFeeApplied(false)
                        .driverMinFeeApplied(false)
                        .shipperChargeAmount(new BigDecimal("100000.00"))
                        .driverPayoutAmount(new BigDecimal("85500.00"))
                        .tossFeeRate(new BigDecimal("0.1000"))
                        .tossFeeAmount(new BigDecimal("10000.00"))
                        .platformGrossRevenue(new BigDecimal("4500.00"))
                        .platformNetRevenue(new BigDecimal("4500.00"))
                        .policyUpdatedAt(policyUpdatedAt)
                        .build()
        );
        when(transportPaymentRepository.save(any(TransportPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.findByOrderId(orderId)).thenAnswer(invocation -> Optional.ofNullable(settlementRef.get()));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement saved = invocation.getArgument(0);
            settlementRef.set(saved);
            return saved;
        });
        when(entityManager.getReference(Order.class, orderId)).thenReturn(org.mockito.Mockito.mock(Order.class));
        when(entityManager.getReference(Users.class, shipperUserId)).thenReturn(org.mockito.Mockito.mock(Users.class));

        TransportPayment saved = paymentLifecycleService.applyPaidFromGatewayTx(tx);

        assertThat(saved.getStatus()).isEqualTo(TransportPaymentStatus.CONFIRMED);
        assertThat(saved.getAmount()).isEqualByComparingTo("100000.00");
        assertThat(saved.getShipperChargeAmountSnapshot()).isEqualByComparingTo("100000.00");
        assertThat(saved.getTossFeeRateSnapshot()).isEqualByComparingTo("0.1000");
        assertThat(saved.getTossFeeAmountSnapshot()).isEqualByComparingTo("10000.00");
        assertThat(saved.getPlatformGrossRevenueSnapshot()).isEqualByComparingTo("4500.00");
        assertThat(saved.getPlatformNetRevenueSnapshot()).isEqualByComparingTo("4500.00");
        assertThat(saved.getFeePolicyAppliedAtSnapshot()).isEqualTo(policyUpdatedAt);

        Settlement settlement = settlementRef.get();
        assertThat(settlement).isNotNull();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(settlement.getFeeDate()).isEqualTo(approvedAt);
        assertThat(settlement.getFeeCompleteDate()).isNotNull();
        assertThat(settlement.getShipperChargeAmountSnapshot()).isEqualTo(100000L);
        assertThat(settlement.getTossFeeAmountSnapshot()).isEqualTo(10000L);
        assertThat(settlement.getPlatformGrossRevenueSnapshot()).isEqualTo(4500L);
        assertThat(settlement.getPlatformNetRevenueSnapshot()).isEqualTo(4500L);
        verify(orderPort).setOrderPaid(orderId);
        verify(orderPort).setOrderConfirmed(orderId);
        verify(driverPayoutService).tryAutoRequestPayoutForOrder(orderId, "PAYMENT_CONFIRMED_BY_TOSS");
    }
}
