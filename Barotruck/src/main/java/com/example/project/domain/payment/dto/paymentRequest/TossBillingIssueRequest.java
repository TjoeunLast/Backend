package com.example.project.domain.payment.dto.paymentRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TossBillingIssueRequest {
    private String authKey;
    private String customerKey;
}
