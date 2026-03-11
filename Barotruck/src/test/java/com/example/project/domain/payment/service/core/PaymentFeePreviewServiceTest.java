package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayChannel;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentProvider;
import com.example.project.domain.payment.dto.paymentRequest.FeePreviewRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicySideResponse;
import com.example.project.domain.payment.port.OrderPort;
import com.example.project.domain.payment.port.UserPort;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFeePreviewServiceTest {

    @Mock
    private OrderPort orderPort;

    @Mock
    private UserPort userPort;

    @Mock
    private FeePolicyService feePolicyService;

    @Mock
    private PromotionEligibilityService promotionEligibilityService;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @InjectMocks
    private PaymentFeePreviewService paymentFeePreviewService;

    @Test
    void previewDraftForShipperReturnsFlatBilateralResponseWithPendingDriverAsNull() {
        Users shipper = user(10L, Role.SHIPPER);
        FeePreviewRequest request = request(
                null,
                null,
                null,
                new BigDecimal("100000"),
                PaymentProvider.TOSS,
                PaymentMethod.CARD,
                PayChannel.CARD
        );

        when(userPort.getRequiredUser(10L)).thenReturn(new UserPort.UserInfo(10L, 1L));
        when(transportPaymentRepository.countByShipperUserIdAndStatusIn(eq(10L), anyList())).thenReturn(0L);
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy("0.1000"));

        FeeBreakdownPreviewResponse response = paymentFeePreviewService.preview(shipper, request);

        assertThat(response.previewMode()).isEqualTo("ORDER_CREATE");
        assertThat(response.baseAmount()).isEqualByComparingTo("100000");
        assertThat(response.postTossBaseAmount()).isEqualByComparingTo("90000");
        assertThat(response.shipperAppliedLevel()).isEqualTo(1L);
        assertThat(response.driverAppliedLevel()).isNull();
        assertThat(response.shipperPromoEligible()).isTrue();
        assertThat(response.shipperPromoApplied()).isTrue();
        assertThat(response.driverPromoEligible()).isNull();
        assertThat(response.driverPromoApplied()).isNull();
        assertThat(response.shipperFeeAmount()).isEqualByComparingTo("2000");
        assertThat(response.shipperChargeAmount()).isEqualByComparingTo("100000");
        assertThat(response.tossFeeAmount()).isEqualByComparingTo("10000");
        assertThat(response.platformGrossRevenue()).isEqualByComparingTo("2000");
        assertThat(response.platformNetRevenue()).isEqualByComparingTo("2000.00");
        assertThat(response.negativeMargin()).isFalse();
    }

    @Test
    void previewExistingOrderUsesPromotionContextsAndReturnsDriverBreakdown() {
        Users shipper = user(10L, Role.SHIPPER);
        FeePreviewRequest request = request(
                55L,
                null,
                null,
                null,
                PaymentProvider.TOSS,
                PaymentMethod.CARD,
                PayChannel.CARD
        );

        when(orderPort.getRequiredSnapshot(55L)).thenReturn(new OrderPort.OrderSnapshot(
                55L,
                10L,
                20L,
                new BigDecimal("100000"),
                "COMPLETED",
                "CARD"
        ));
        when(promotionEligibilityService.getShipperFirstPaymentContext(55L))
                .thenReturn(new PromotionEligibilityService.PromotionContext(10L, 1L, false));
        when(promotionEligibilityService.getDriverFirstTransportContext(55L))
                .thenReturn(new PromotionEligibilityService.PromotionContext(20L, 2L, true));
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy("0.1000"));

        FeeBreakdownPreviewResponse response = paymentFeePreviewService.preview(shipper, request);

        assertThat(response.previewMode()).isEqualTo("PAYMENT_ENTRY");
        assertThat(response.shipperAppliedLevel()).isEqualTo(1L);
        assertThat(response.driverAppliedLevel()).isEqualTo(2L);
        assertThat(response.shipperPromoEligible()).isFalse();
        assertThat(response.driverPromoEligible()).isTrue();
        assertThat(response.shipperPromoApplied()).isFalse();
        assertThat(response.driverPromoApplied()).isTrue();
        assertThat(response.postTossBaseAmount()).isEqualByComparingTo("90000");
        assertThat(response.shipperFeeAmount()).isEqualByComparingTo("2000");
        assertThat(response.driverFeeAmount()).isEqualByComparingTo("2000");
        assertThat(response.driverPayoutAmount()).isEqualByComparingTo("86000");
        assertThat(response.tossFeeAmount()).isEqualByComparingTo("10000");
        assertThat(response.platformGrossRevenue()).isEqualByComparingTo("4000");
        assertThat(response.platformNetRevenue()).isEqualByComparingTo("4000");
        assertThat(response.negativeMargin()).isFalse();
    }

    @Test
    void previewExistingOrderRejectsOtherShipperAccess() {
        Users shipper = user(10L, Role.SHIPPER);
        FeePreviewRequest request = request(
                55L,
                null,
                null,
                null,
                PaymentProvider.TOSS,
                PaymentMethod.CARD,
                PayChannel.CARD
        );

        when(orderPort.getRequiredSnapshot(55L)).thenReturn(new OrderPort.OrderSnapshot(
                55L,
                99L,
                20L,
                new BigDecimal("100000"),
                "COMPLETED",
                "CARD"
        ));

        assertThatThrownBy(() -> paymentFeePreviewService.preview(shipper, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shipper can preview only own order");
    }

    private FeePolicyResponse policy(String tossRate) {
        return FeePolicyResponse.builder()
                .policyConfigId(12L)
                .shipperSide(FeePolicySideResponse.builder()
                        .level0Rate(new BigDecimal("0.0250"))
                        .level1Rate(new BigDecimal("0.0200"))
                        .level2Rate(new BigDecimal("0.0180"))
                        .level3PlusRate(new BigDecimal("0.0150"))
                        .build())
                .driverSide(FeePolicySideResponse.builder()
                        .level0Rate(new BigDecimal("0.0250"))
                        .level1Rate(new BigDecimal("0.0200"))
                        .level2Rate(new BigDecimal("0.0180"))
                        .level3PlusRate(new BigDecimal("0.0150"))
                        .build())
                .shipperFirstPaymentPromoRate(new BigDecimal("0.0150"))
                .driverFirstTransportPromoRate(new BigDecimal("0.0150"))
                .tossRate(new BigDecimal(tossRate))
                .minFee(new BigDecimal("2000"))
                .updatedAt(LocalDateTime.parse("2026-03-11T09:00:00"))
                .build();
    }

    private Users user(Long userId, Role role) {
        return Users.builder()
                .userId(userId)
                .role(role)
                .email(role.name().toLowerCase() + "@test.com")
                .build();
    }

    private FeePreviewRequest request(
            Long orderId,
            Long shipperUserId,
            Long driverUserId,
            BigDecimal baseAmount,
            PaymentProvider paymentProvider,
            PaymentMethod paymentMethod,
            PayChannel payChannel
    ) {
        FeePreviewRequest request = new FeePreviewRequest();
        ReflectionTestUtils.setField(request, "orderId", orderId);
        ReflectionTestUtils.setField(request, "shipperUserId", shipperUserId);
        ReflectionTestUtils.setField(request, "driverUserId", driverUserId);
        ReflectionTestUtils.setField(request, "baseAmount", baseAmount);
        ReflectionTestUtils.setField(request, "paymentProvider", paymentProvider);
        ReflectionTestUtils.setField(request, "paymentMethod", paymentMethod);
        ReflectionTestUtils.setField(request, "payChannel", payChannel);
        return request;
    }
}
