package com.example.project.domain.payment.domain;

public enum TransportPaymentStatus {
    // 결제 객체가 생성된 초기 상태
    READY,
    // 화주 결제 완료
    PAID,
    // 차주 결제 확인 완료
    CONFIRMED,
    // 결제 이의제기 접수 상태
    DISPUTED,
    // 관리자 검토 보류
    ADMIN_HOLD,
    // 관리자 강제 확정
    ADMIN_FORCE_CONFIRMED,
    // 관리자 반려
    ADMIN_REJECTED,
    // 결제 취소
    CANCELLED
}
