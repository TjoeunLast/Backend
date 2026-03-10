package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.FeeAutoChargeAttempt;
import lombok.Builder;

import java.util.List;

@Builder
public record FeeAutoChargeAttemptListResponse(
        Long shipperUserId,
        Long invoiceId,
        Integer limit,
        Integer count,
        List<FeeAutoChargeAttemptResponse> attempts
) {
    public static FeeAutoChargeAttemptListResponse of(
            Long shipperUserId,
            Long invoiceId,
            Integer limit,
            List<FeeAutoChargeAttempt> attempts
    ) {
        List<FeeAutoChargeAttemptResponse> mappedAttempts = attempts == null
                ? List.of()
                : attempts.stream().map(FeeAutoChargeAttemptResponse::from).toList();

        return FeeAutoChargeAttemptListResponse.builder()
                .shipperUserId(shipperUserId)
                .invoiceId(invoiceId)
                .limit(limit)
                .count(mappedAttempts.size())
                .attempts(mappedAttempts)
                .build();
    }
}
