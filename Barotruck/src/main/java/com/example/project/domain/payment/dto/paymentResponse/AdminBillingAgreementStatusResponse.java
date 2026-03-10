package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeAutoChargeAttempt;
import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminBillingAgreementStatusResponse(
        Long shipperUserId,
        ShipperBillingAgreementResponse agreement,
        FeeAutoChargeAttemptResponse latestAttempt,
        long totalAttemptCount,
        long successAttemptCount,
        long failedAttemptCount,
        LocalDateTime lastAttemptedAt,
        List<FeeAutoChargeAttemptResponse> recentAttempts
) {
    public static AdminBillingAgreementStatusResponse of(
            Long shipperUserId,
            ShipperBillingAgreement agreement,
            List<FeeAutoChargeAttempt> recentAttempts,
            long totalAttemptCount,
            long successAttemptCount,
            long failedAttemptCount
    ) {
        List<FeeAutoChargeAttemptResponse> mappedAttempts = recentAttempts == null
                ? List.of()
                : recentAttempts.stream().map(FeeAutoChargeAttemptResponse::from).toList();
        FeeAutoChargeAttempt latestAttempt = recentAttempts == null || recentAttempts.isEmpty()
                ? null
                : recentAttempts.get(0);

        return AdminBillingAgreementStatusResponse.builder()
                .shipperUserId(shipperUserId)
                .agreement(ShipperBillingAgreementResponse.from(agreement))
                .latestAttempt(latestAttempt == null ? null : FeeAutoChargeAttemptResponse.from(latestAttempt))
                .totalAttemptCount(totalAttemptCount)
                .successAttemptCount(successAttemptCount)
                .failedAttemptCount(failedAttemptCount)
                .lastAttemptedAt(latestAttempt == null ? null : latestAttempt.getAttemptedAt())
                .recentAttempts(mappedAttempts)
                .build();
    }
}
