package com.example.project.domain.payment.domain.paymentEnum;

public enum GatewayTxStatus {
    // 결제 준비 완료
    PREPARED,
    // PG 승인 완료
    CONFIRMED,
    // PG 승인 실패
    FAILED,
    // PG 취소 완료
    CANCELED
}
