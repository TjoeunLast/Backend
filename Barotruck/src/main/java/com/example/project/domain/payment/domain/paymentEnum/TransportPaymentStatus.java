package com.example.project.domain.payment.domain.paymentEnum;

public enum TransportPaymentStatus {
    // 결제 레코드 생성 직후 초기 상태
    READY,
    // 화주 결제 완료 상태
    PAID,
    // 차주가 결제를 확인한 상태
    CONFIRMED,
    // 결제 이의제기 접수 상태
    DISPUTED,
    // 관리자 보류 상태
    ADMIN_HOLD,
    // 관리자 강제 확정 상태
    ADMIN_FORCE_CONFIRMED,
    // 관리자 반려 상태
    ADMIN_REJECTED,
    // 결제 취소 상태
    CANCELLED
}
