package com.example.project.domain.payment.domain;

public enum PaymentDisputeReason {
    // 청구 금액 불일치
    PRICE_MISMATCH,
    // 차주 실수령 금액 불일치
    RECEIVED_AMOUNT_MISMATCH,
    // 인수증/증빙 누락
    PROOF_MISSING,
    // 부정 거래 의심
    FRAUD_SUSPECTED,
    // 기타 사유
    OTHER
}
