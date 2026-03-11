package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicySideResponse;
import com.example.project.domain.payment.port.UserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceFeeCalculationServiceTest {

    @Mock
    private FeePolicyService feePolicyService;

    @Mock
    private UserPort userPort;

    @Mock
    private PromotionEligibilityService promotionEligibilityService;

    @InjectMocks
    private MarketplaceFeeCalculationService marketplaceFeeCalculationService;

    @Test
    void calculate_usesPostTossBaseForBothSideFees() {
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy(
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.1000",
                "2000"
        ));
        when(promotionEligibilityService.getShipperFirstPaymentContext(77L))
                .thenReturn(new PromotionEligibilityService.PromotionContext(10L, 1L, false));
        when(promotionEligibilityService.getDriverFirstTransportContext(77L))
                .thenReturn(new PromotionEligibilityService.PromotionContext(20L, 0L, true));

        FeeBreakdownPreviewResponse result = marketplaceFeeCalculationService.calculate(
                MarketplaceFeeCalculationService.CalculationCommand.builder()
                        .orderId(77L)
                        .baseAmount(new BigDecimal("100000"))
                        .includeTossFee(true)
                        .build()
        );

        assertThat(result.shipperAppliedLevel()).isEqualTo(1L);
        assertThat(result.driverAppliedLevel()).isEqualTo(0L);
        assertThat(result.postTossBaseAmount()).isEqualByComparingTo("90000");
        assertThat(result.shipperFeeRate()).isEqualByComparingTo("0.0200");
        assertThat(result.driverFeeRate()).isEqualByComparingTo("0.0150");
        assertThat(result.shipperFeeAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.driverFeeAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.shipperPromoApplied()).isFalse();
        assertThat(result.driverPromoApplied()).isTrue();
        assertThat(result.shipperChargeAmount()).isEqualByComparingTo("100000.00");
        assertThat(result.driverPayoutAmount()).isEqualByComparingTo("86000.00");
        assertThat(result.tossFeeAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.platformGrossRevenue()).isEqualByComparingTo("4000.00");
        assertThat(result.platformNetRevenue()).isEqualByComparingTo("4000.00");

        verifyNoInteractions(userPort);
    }

    @Test
    void calculate_usesExplicitLevelsAndPromoFlagsWithoutPromotionLookup() {
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy(
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.1000",
                "2000"
        ));

        FeeBreakdownPreviewResponse result = marketplaceFeeCalculationService.calculate(
                MarketplaceFeeCalculationService.CalculationCommand.builder()
                        .baseAmount(new BigDecimal("200000"))
                        .shipperUserLevel(2L)
                        .driverUserLevel(1L)
                        .shipperPromoEligible(true)
                        .driverPromoEligible(false)
                        .includeTossFee(false)
                        .build()
        );

        assertThat(result.shipperAppliedLevel()).isEqualTo(2L);
        assertThat(result.driverAppliedLevel()).isEqualTo(1L);
        assertThat(result.shipperFeeRate()).isEqualByComparingTo("0.0150");
        assertThat(result.driverFeeRate()).isEqualByComparingTo("0.0200");
        assertThat(result.shipperFeeAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.driverFeeAmount()).isEqualByComparingTo("4000.00");
        assertThat(result.shipperPromoApplied()).isTrue();
        assertThat(result.driverPromoApplied()).isFalse();
        assertThat(result.tossFeeRate()).isEqualByComparingTo("0.0000");
        assertThat(result.tossFeeAmount()).isEqualByComparingTo("0.00");
        assertThat(result.platformGrossRevenue()).isEqualByComparingTo("7000.00");
        assertThat(result.platformNetRevenue()).isEqualByComparingTo("7000.00");
        assertThat(result.driverPayoutAmount()).isEqualByComparingTo("193000.00");

        verifyNoInteractions(promotionEligibilityService);
        verifyNoInteractions(userPort);
    }

    @Test
    void calculate_roundsWholeAmountsAndUsesPolicyTossRate() {
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy(
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0100",
                "0.0100",
                "0.0350",
                "0"
        ));

        FeeBreakdownPreviewResponse result = marketplaceFeeCalculationService.calculate(
                MarketplaceFeeCalculationService.CalculationCommand.builder()
                        .baseAmount(new BigDecimal("33333"))
                        .shipperUserLevel(0L)
                        .driverUserLevel(0L)
                        .shipperPromoEligible(false)
                        .driverPromoEligible(false)
                        .includeTossFee(true)
                        .build()
        );

        assertThat(result.postTossBaseAmount()).isEqualByComparingTo("32166.00");
        assertThat(result.shipperFeeAmount()).isEqualByComparingTo("804.00");
        assertThat(result.driverFeeAmount()).isEqualByComparingTo("804.00");
        assertThat(result.shipperChargeAmount()).isEqualByComparingTo("33333.00");
        assertThat(result.tossFeeRate()).isEqualByComparingTo("0.0350");
        assertThat(result.tossFeeAmount()).isEqualByComparingTo("1167.00");
        assertThat(result.platformGrossRevenue()).isEqualByComparingTo("1608.00");
        assertThat(result.platformNetRevenue()).isEqualByComparingTo("1608.00");

        verifyNoInteractions(promotionEligibilityService);
        verifyNoInteractions(userPort);
    }

    @Test
    void calculate_readsUserLevelsWhenOrderContextIsMissing() {
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy(
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0250",
                "0.0200",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.0150",
                "0.1000",
                "0"
        ));
        when(userPort.getRequiredUser(10L)).thenReturn(new UserPort.UserInfo(10L, 3L));
        when(userPort.getRequiredUser(20L)).thenReturn(new UserPort.UserInfo(20L, 1L));

        FeeBreakdownPreviewResponse result = marketplaceFeeCalculationService.calculate(
                MarketplaceFeeCalculationService.CalculationCommand.builder()
                        .baseAmount(new BigDecimal("120000"))
                        .shipperUserId(10L)
                        .driverUserId(20L)
                        .shipperPromoEligible(false)
                        .driverPromoEligible(false)
                        .includeTossFee(false)
                        .build()
        );

        assertThat(result.shipperAppliedLevel()).isEqualTo(3L);
        assertThat(result.driverAppliedLevel()).isEqualTo(1L);
        assertThat(result.shipperFeeAmount()).isEqualByComparingTo("1800.00");
        assertThat(result.driverFeeAmount()).isEqualByComparingTo("2400.00");
        assertThat(result.driverPayoutAmount()).isEqualByComparingTo("115800.00");

        verifyNoInteractions(promotionEligibilityService);
    }

    @Test
    void calculate_capsSideFeesToRemainingPostTossBase() {
        when(feePolicyService.getCurrentPolicy()).thenReturn(policy(
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.0250",
                "0.1000",
                "2000"
        ));

        FeeBreakdownPreviewResponse result = marketplaceFeeCalculationService.calculate(
                MarketplaceFeeCalculationService.CalculationCommand.builder()
                        .baseAmount(new BigDecimal("1000"))
                        .shipperUserLevel(0L)
                        .driverUserLevel(0L)
                        .shipperPromoEligible(false)
                        .driverPromoEligible(false)
                        .includeTossFee(true)
                        .build()
        );

        assertThat(result.postTossBaseAmount()).isEqualByComparingTo("900.00");
        assertThat(result.shipperFeeAmount()).isEqualByComparingTo("450.00");
        assertThat(result.driverFeeAmount()).isEqualByComparingTo("450.00");
        assertThat(result.driverPayoutAmount()).isEqualByComparingTo("0.00");
        assertThat(result.platformGrossRevenue()).isEqualByComparingTo("900.00");
        assertThat(result.platformNetRevenue()).isEqualByComparingTo("900.00");
    }

    private FeePolicyResponse policy(
            String shipperLevel0Rate,
            String shipperLevel1Rate,
            String shipperLevel2Rate,
            String shipperLevel3PlusRate,
            String driverLevel0Rate,
            String driverLevel1Rate,
            String driverLevel2Rate,
            String driverLevel3PlusRate,
            String shipperPromoRate,
            String driverPromoRate,
            String tossRate,
            String minFee
    ) {
        return FeePolicyResponse.builder()
                .shipperSide(FeePolicySideResponse.builder()
                        .level0Rate(new BigDecimal(shipperLevel0Rate))
                        .level1Rate(new BigDecimal(shipperLevel1Rate))
                        .level2Rate(new BigDecimal(shipperLevel2Rate))
                        .level3PlusRate(new BigDecimal(shipperLevel3PlusRate))
                        .build())
                .driverSide(FeePolicySideResponse.builder()
                        .level0Rate(new BigDecimal(driverLevel0Rate))
                        .level1Rate(new BigDecimal(driverLevel1Rate))
                        .level2Rate(new BigDecimal(driverLevel2Rate))
                        .level3PlusRate(new BigDecimal(driverLevel3PlusRate))
                        .build())
                .shipperFirstPaymentPromoRate(new BigDecimal(shipperPromoRate))
                .driverFirstTransportPromoRate(new BigDecimal(driverPromoRate))
                .tossRate(new BigDecimal(tossRate))
                .minFee(new BigDecimal(minFee))
                .build();
    }
}
