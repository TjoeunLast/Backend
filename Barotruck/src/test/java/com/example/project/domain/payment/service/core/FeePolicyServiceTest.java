package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeePolicyConfig;
import com.example.project.domain.payment.dto.paymentRequest.FeePolicySideRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateFeePolicyRequest;
import com.example.project.domain.payment.dto.paymentRequest.UpdateLevelFeeRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeePolicyResponse;
import com.example.project.domain.payment.dto.paymentResponse.LevelFeePolicyResponse;
import com.example.project.domain.payment.repository.FeePolicyConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FeePolicyServiceTest {

    @Mock
    private FeePolicyConfigRepository feePolicyConfigRepository;

    @InjectMocks
    private FeePolicyService feePolicyService;

    @Test
    void getCurrentPolicy_readsLegacyShipperFieldsAndDefaultsDriverSide() {
        FeePolicyConfig legacyOnly = FeePolicyConfig.builder()
                .level0Rate(new BigDecimal("0.0500"))
                .level1Rate(new BigDecimal("0.0400"))
                .level2Rate(new BigDecimal("0.0300"))
                .level3PlusRate(new BigDecimal("0.0300"))
                .firstPaymentPromoRate(new BigDecimal("0.0300"))
                .minFee(new BigDecimal("2000.00"))
                .build();
        ReflectionTestUtils.setField(legacyOnly, "createdAt", LocalDateTime.of(2026, 3, 11, 10, 30));

        when(feePolicyConfigRepository.findTopByOrderByPolicyIdDesc()).thenReturn(Optional.of(legacyOnly));

        FeePolicyResponse response = feePolicyService.getCurrentPolicy();

        assertThat(response.level0Rate()).isEqualByComparingTo("0.0500");
        assertThat(response.shipperSide().level1Rate()).isEqualByComparingTo("0.0400");
        assertThat(response.driverSide().level0Rate()).isEqualByComparingTo("0.0250");
        assertThat(response.shipperFirstPaymentPromoRate()).isEqualByComparingTo("0.0300");
        assertThat(response.driverFirstTransportPromoRate()).isEqualByComparingTo("0.0150");
        assertThat(response.tossRate()).isEqualByComparingTo("0.1000");
    }

    @Test
    void updatePolicy_acceptsLegacyShipperPayloadAndPreservesDriverAndTossSettings() {
        FeePolicyConfig current = FeePolicyConfig.builder()
                .level0Rate(new BigDecimal("0.0250"))
                .level1Rate(new BigDecimal("0.0200"))
                .level2Rate(new BigDecimal("0.0180"))
                .level3PlusRate(new BigDecimal("0.0150"))
                .firstPaymentPromoRate(new BigDecimal("0.0150"))
                .shipperLevel0Rate(new BigDecimal("0.0250"))
                .shipperLevel1Rate(new BigDecimal("0.0200"))
                .shipperLevel2Rate(new BigDecimal("0.0180"))
                .shipperLevel3PlusRate(new BigDecimal("0.0150"))
                .driverLevel0Rate(new BigDecimal("0.0220"))
                .driverLevel1Rate(new BigDecimal("0.0190"))
                .driverLevel2Rate(new BigDecimal("0.0170"))
                .driverLevel3PlusRate(new BigDecimal("0.0150"))
                .shipperFirstPaymentPromoRate(new BigDecimal("0.0150"))
                .driverFirstTransportPromoRate(new BigDecimal("0.0120"))
                .tossRate(new BigDecimal("0.0310"))
                .minFee(new BigDecimal("2000.00"))
                .build();

        when(feePolicyConfigRepository.findTopByOrderByPolicyIdDesc()).thenReturn(Optional.of(current));
        when(feePolicyConfigRepository.save(any(FeePolicyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateFeePolicyRequest request = new UpdateFeePolicyRequest();
        request.setLevel0Rate(new BigDecimal("0.0240"));
        request.setLevel1Rate(new BigDecimal("0.0210"));
        request.setLevel2Rate(new BigDecimal("0.0190"));
        request.setLevel3PlusRate(new BigDecimal("0.0160"));
        request.setFirstPaymentPromoRate(new BigDecimal("0.0140"));
        request.setMinFee(new BigDecimal("2500"));

        FeePolicyResponse response = feePolicyService.updatePolicy(request);

        ArgumentCaptor<FeePolicyConfig> captor = ArgumentCaptor.forClass(FeePolicyConfig.class);
        verify(feePolicyConfigRepository).save(captor.capture());

        FeePolicyConfig saved = captor.getValue();
        assertThat(saved.getShipperLevel0Rate()).isEqualByComparingTo("0.0240");
        assertThat(saved.getDriverLevel0Rate()).isEqualByComparingTo("0.0220");
        assertThat(saved.getDriverFirstTransportPromoRate()).isEqualByComparingTo("0.0120");
        assertThat(saved.getTossRate()).isEqualByComparingTo("0.0310");
        assertThat(saved.getFirstPaymentPromoRate()).isEqualByComparingTo("0.0140");

        assertThat(response.shipperSide().level3PlusRate()).isEqualByComparingTo("0.0160");
        assertThat(response.driverSide().level1Rate()).isEqualByComparingTo("0.0190");
        assertThat(response.shipperFirstPaymentPromoRate()).isEqualByComparingTo("0.0140");
        assertThat(response.driverFirstTransportPromoRate()).isEqualByComparingTo("0.0120");
        assertThat(response.tossRate()).isEqualByComparingTo("0.0310");
        assertThat(response.minFee()).isEqualByComparingTo("2500.00");
    }

    @Test
    void updateLevelPolicy_updatesRequestedDriverSideOnly() {
        FeePolicyConfig current = FeePolicyConfig.builder()
                .level0Rate(new BigDecimal("0.0250"))
                .level1Rate(new BigDecimal("0.0200"))
                .level2Rate(new BigDecimal("0.0180"))
                .level3PlusRate(new BigDecimal("0.0150"))
                .firstPaymentPromoRate(new BigDecimal("0.0150"))
                .shipperLevel0Rate(new BigDecimal("0.0250"))
                .shipperLevel1Rate(new BigDecimal("0.0200"))
                .shipperLevel2Rate(new BigDecimal("0.0180"))
                .shipperLevel3PlusRate(new BigDecimal("0.0150"))
                .driverLevel0Rate(new BigDecimal("0.0240"))
                .driverLevel1Rate(new BigDecimal("0.0200"))
                .driverLevel2Rate(new BigDecimal("0.0180"))
                .driverLevel3PlusRate(new BigDecimal("0.0150"))
                .shipperFirstPaymentPromoRate(new BigDecimal("0.0150"))
                .driverFirstTransportPromoRate(new BigDecimal("0.0110"))
                .tossRate(new BigDecimal("0.0300"))
                .minFee(new BigDecimal("2000.00"))
                .build();

        when(feePolicyConfigRepository.findTopByOrderByPolicyIdDesc()).thenReturn(Optional.of(current));
        when(feePolicyConfigRepository.save(any(FeePolicyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateLevelFeeRequest request = new UpdateLevelFeeRequest();
        ReflectionTestUtils.setField(request, "level", 1L);
        ReflectionTestUtils.setField(request, "rate", new BigDecimal("0.0175"));
        ReflectionTestUtils.setField(request, "side", "driver");

        LevelFeePolicyResponse response = feePolicyService.updateLevelPolicy(request);

        ArgumentCaptor<FeePolicyConfig> captor = ArgumentCaptor.forClass(FeePolicyConfig.class);
        verify(feePolicyConfigRepository).save(captor.capture());

        FeePolicyConfig saved = captor.getValue();
        assertThat(saved.getDriverLevel1Rate()).isEqualByComparingTo("0.0175");
        assertThat(saved.getShipperLevel1Rate()).isEqualByComparingTo("0.0200");

        assertThat(response.side()).isEqualTo("driver");
        assertThat(response.shipperRate()).isEqualByComparingTo("0.0200");
        assertThat(response.driverRate()).isEqualByComparingTo("0.0175");
        assertThat(response.rate()).isEqualByComparingTo("0.0200");
    }
}
