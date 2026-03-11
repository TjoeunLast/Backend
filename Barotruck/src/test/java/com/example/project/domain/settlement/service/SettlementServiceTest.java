package com.example.project.domain.settlement.service;

import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.dto.SettlementResponse;
import com.example.project.domain.settlement.dto.UpdateSettlementStatusRequest;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private PaymentDisputeRepository paymentDisputeRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    void updateSettlementStatus_waitMovesPaymentToAdminHoldAndCreatesDispute() {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getOrderId()).thenReturn(100L);
        when(order.getDriverNo()).thenReturn(300L);
        Settlement settlement = Settlement.builder()
                .id(1L)
                .order(order)
                .status(SettlementStatus.READY)
                .build();
        TransportPayment payment = TransportPayment.ready(
                100L,
                200L,
                300L,
                new BigDecimal("120000"),
                new BigDecimal("0.1000"),
                new BigDecimal("12000"),
                new BigDecimal("108000"),
                PaymentMethod.CARD
        );
        ReflectionTestUtils.setField(payment, "paymentId", 500L);
        Users admin = adminUser(900L);
        UpdateSettlementStatusRequest request = request(SettlementStatus.WAIT, "hold review");

        when(settlementRepository.findByOrderId(100L)).thenReturn(Optional.of(settlement));
        when(transportPaymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(paymentDisputeRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(usersRepository.findById(300L)).thenReturn(Optional.empty());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResponse response = settlementService.updateSettlementStatus(100L, request, admin);

        assertThat(response.status()).isEqualTo(SettlementStatus.WAIT.name());
        assertThat(response.baseAmount()).isEqualTo(108000L);
        assertThat(response.shipperChargeAmount()).isEqualTo(120000L);
        assertThat(response.shipperFeeAmount()).isEqualTo(12000L);
        assertThat(response.driverPayoutAmount()).isEqualTo(108000L);
        assertThat(payment.getStatus()).isEqualTo(TransportPaymentStatus.ADMIN_HOLD);
        assertThat(settlement.getFeeCompleteDate()).isNull();
        verify(paymentDisputeRepository).save(any(PaymentDispute.class));
    }

    @Test
    void updateSettlementStatus_completedForceConfirmsPaymentAndExistingDispute() {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getOrderId()).thenReturn(101L);
        when(order.getDriverNo()).thenReturn(301L);
        Settlement settlement = Settlement.builder()
                .id(2L)
                .order(order)
                .status(SettlementStatus.WAIT)
                .build();
        TransportPayment payment = TransportPayment.ready(
                101L,
                201L,
                301L,
                new BigDecimal("150000"),
                new BigDecimal("0.1000"),
                new BigDecimal("15000"),
                new BigDecimal("135000"),
                PaymentMethod.CARD
        );
        ReflectionTestUtils.setField(payment, "paymentId", 501L);
        payment.updateStatus(TransportPaymentStatus.DISPUTED);
        PaymentDispute dispute = PaymentDispute.create(
                101L,
                501L,
                301L,
                900L,
                com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason.OTHER,
                "existing",
                null
        );
        Users admin = adminUser(900L);
        UpdateSettlementStatusRequest request = request(SettlementStatus.COMPLETED, "resolved");

        when(settlementRepository.findByOrderId(101L)).thenReturn(Optional.of(settlement));
        when(transportPaymentRepository.findByOrderId(101L)).thenReturn(Optional.of(payment));
        when(paymentDisputeRepository.findByOrderId(101L)).thenReturn(Optional.of(dispute));
        when(usersRepository.findById(301L)).thenReturn(Optional.empty());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResponse response = settlementService.updateSettlementStatus(101L, request, admin);

        assertThat(response.status()).isEqualTo(SettlementStatus.COMPLETED.name());
        assertThat(payment.getStatus()).isEqualTo(TransportPaymentStatus.ADMIN_FORCE_CONFIRMED);
        assertThat(payment.getConfirmedAt()).isNotNull();
        assertThat(settlement.getFeeCompleteDate()).isNotNull();
        assertThat(response.shipperChargeAmount()).isEqualTo(150000L);
        assertThat(response.platformGrossRevenue()).isEqualTo(15000L);
        assertThat(dispute.getStatus()).isEqualTo(PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED);
        assertThat(dispute.getAdminMemo()).isEqualTo("resolved");
    }

    private Users adminUser(Long userId) {
        return Users.builder()
                .userId(userId)
                .role(Role.ADMIN)
                .email("admin@test.com")
                .build();
    }

    private UpdateSettlementStatusRequest request(SettlementStatus status, String adminMemo) {
        UpdateSettlementStatusRequest request = new UpdateSettlementStatusRequest();
        ReflectionTestUtils.setField(request, "status", status);
        ReflectionTestUtils.setField(request, "adminMemo", adminMemo);
        return request;
    }
}
