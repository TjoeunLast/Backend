package com.example.project.domain.payment.dto.paymentResponse;

import lombok.Builder;

@Builder
public record TossBillingContextResponse(
        String clientKey,
        String customerKey,
        String successUrl,
        String failUrl
) {
}
