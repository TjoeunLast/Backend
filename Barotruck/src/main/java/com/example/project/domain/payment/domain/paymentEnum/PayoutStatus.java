package com.example.project.domain.payment.domain.paymentEnum;

public enum PayoutStatus {
    // 지급 대상 집계 완료
    READY,
    // 지급 요청 전송 완료
    REQUESTED,
    // 지급 완료
    COMPLETED,
    // 지급 실패
    FAILED,
    // 재시도 진행 중
    RETRYING
}
