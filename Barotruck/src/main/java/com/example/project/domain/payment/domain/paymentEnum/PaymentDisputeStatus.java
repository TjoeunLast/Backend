package com.example.project.domain.payment.domain.paymentEnum;

public enum PaymentDisputeStatus {
    // 이의제기 등록 직후 대기 상태
    PENDING,
    // 관리자 검토 보류
    ADMIN_HOLD,
    // 관리자 강제 확정 처리
    ADMIN_FORCE_CONFIRMED,
    // 관리자 반려 처리
    ADMIN_REJECTED
}
