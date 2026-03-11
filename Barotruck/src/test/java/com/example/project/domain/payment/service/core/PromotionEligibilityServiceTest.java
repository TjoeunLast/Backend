package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionEligibilityServiceTest {

    @Mock
    private OrderPort orderPort;

    @Mock
    private UserPort userPort;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @InjectMocks
    private PromotionEligibilityService promotionEligibilityService;

    @Test
    void resolveShipperFirstPaymentContext_marksPromoWhenNoPriorPromoExists() {
        TransportPayment payment = TransportPayment.builder()
                .orderId(100L)
                .shipperUserId(10L)
                .driverUserId(20L)
                .amount(new BigDecimal("100000"))
                .feeRateSnapshot(new BigDecimal("0.0250"))
                .feeAmountSnapshot(new BigDecimal("2500"))
                .netAmountSnapshot(new BigDecimal("97500"))
                .method(PaymentMethod.CARD)
                .status(TransportPaymentStatus.READY)
                .createdAt(LocalDateTime.now())
                .build();

        when(userPort.lockRequiredUser(10L)).thenReturn(new UserPort.UserInfo(10L, 0L));
        when(transportPaymentRepository.existsByShipperUserIdAndFirstPaymentPromoAppliedTrueAndOrderIdNot(10L, 100L))
                .thenReturn(false);

        PromotionEligibilityService.PromotionContext result =
                promotionEligibilityService.resolveShipperFirstPaymentContext(payment);

        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.userLevel()).isEqualTo(0L);
        assertThat(result.promoEligible()).isTrue();
        assertThat(payment.isFirstPaymentPromoApplied()).isTrue();
    }

    @Test
    void resolveShipperFirstPaymentContext_isIdempotentWhenCurrentPaymentAlreadyApplied() {
        TransportPayment payment = TransportPayment.builder()
                .orderId(101L)
                .shipperUserId(11L)
                .driverUserId(21L)
                .amount(new BigDecimal("120000"))
                .feeRateSnapshot(new BigDecimal("0.0200"))
                .feeAmountSnapshot(new BigDecimal("2400"))
                .netAmountSnapshot(new BigDecimal("117600"))
                .method(PaymentMethod.TRANSFER)
                .status(TransportPaymentStatus.PAID)
                .createdAt(LocalDateTime.now())
                .firstPaymentPromoApplied(true)
                .build();

        when(userPort.lockRequiredUser(11L)).thenReturn(new UserPort.UserInfo(11L, 1L));

        PromotionEligibilityService.PromotionContext result =
                promotionEligibilityService.resolveShipperFirstPaymentContext(payment);

        assertThat(result.promoEligible()).isTrue();
        verify(transportPaymentRepository, never())
                .existsByShipperUserIdAndFirstPaymentPromoAppliedTrueAndOrderIdNot(11L, 101L);
    }

    @Test
    void getShipperFirstPaymentContext_reusesCurrentPaymentFlagForReadOnlyLookup() {
        OrderPort.OrderSnapshot snapshot = new OrderPort.OrderSnapshot(
                102L,
                12L,
                22L,
                new BigDecimal("130000"),
                "COMPLETED",
                "CARD"
        );
        TransportPayment payment = TransportPayment.builder()
                .orderId(102L)
                .shipperUserId(12L)
                .driverUserId(22L)
                .amount(new BigDecimal("130000"))
                .feeRateSnapshot(new BigDecimal("0.0150"))
                .feeAmountSnapshot(new BigDecimal("1950"))
                .netAmountSnapshot(new BigDecimal("128050"))
                .method(PaymentMethod.CARD)
                .status(TransportPaymentStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .firstPaymentPromoApplied(true)
                .build();

        when(orderPort.getRequiredSnapshot(102L)).thenReturn(snapshot);
        when(userPort.getRequiredUser(12L)).thenReturn(new UserPort.UserInfo(12L, 2L));
        when(transportPaymentRepository.findByOrderId(102L)).thenReturn(Optional.of(payment));

        PromotionEligibilityService.PromotionContext result =
                promotionEligibilityService.getShipperFirstPaymentContext(102L);

        assertThat(result.userLevel()).isEqualTo(2L);
        assertThat(result.promoEligible()).isTrue();
        verify(transportPaymentRepository, never())
                .existsByShipperUserIdAndFirstPaymentPromoAppliedTrueAndOrderIdNot(12L, 102L);
    }

    @Test
    void applyDriverFirstTransportPromotion_marksPromoWhenNoPriorPromoExists() {
        DriverPayoutItem item = DriverPayoutItem.builder()
                .orderId(200L)
                .driverUserId(30L)
                .batch(batch(1L))
                .netAmount(new BigDecimal("90000"))
                .status(PayoutStatus.READY)
                .retryCount(0)
                .build();

        when(userPort.lockRequiredUser(30L)).thenReturn(new UserPort.UserInfo(30L, 0L));
        when(driverPayoutItemRepository.existsByDriverUserIdAndFirstTransportPromoAppliedTrueAndOrderIdNot(30L, 200L))
                .thenReturn(false);

        PromotionEligibilityService.PromotionContext result =
                promotionEligibilityService.applyDriverFirstTransportPromotion(item);

        assertThat(result.userId()).isEqualTo(30L);
        assertThat(result.promoEligible()).isTrue();
        assertThat(item.isFirstTransportPromoApplied()).isTrue();
    }

    @Test
    void getDriverFirstTransportContext_returnsFalseWhenAnotherOrderAlreadyConsumedPromo() {
        OrderPort.OrderSnapshot snapshot = new OrderPort.OrderSnapshot(
                201L,
                13L,
                31L,
                new BigDecimal("140000"),
                "COMPLETED",
                "CARD"
        );

        when(orderPort.getRequiredSnapshot(201L)).thenReturn(snapshot);
        when(userPort.getRequiredUser(31L)).thenReturn(new UserPort.UserInfo(31L, 3L));
        when(driverPayoutItemRepository.findByOrderId(201L)).thenReturn(Optional.empty());
        when(driverPayoutItemRepository.existsByDriverUserIdAndFirstTransportPromoAppliedTrueAndOrderIdNot(31L, 201L))
                .thenReturn(true);

        PromotionEligibilityService.PromotionContext result =
                promotionEligibilityService.getDriverFirstTransportContext(201L);

        assertThat(result.userLevel()).isEqualTo(3L);
        assertThat(result.promoEligible()).isFalse();
    }

    private DriverPayoutBatch batch(Long batchId) {
        return DriverPayoutBatch.builder()
                .batchId(batchId)
                .batchDate(LocalDate.of(2026, 3, 11))
                .status(PayoutStatus.READY)
                .build();
    }
}
