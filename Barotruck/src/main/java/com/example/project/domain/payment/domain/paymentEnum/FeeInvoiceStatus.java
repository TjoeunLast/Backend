package com.example.project.domain.payment.domain.paymentEnum;

public enum FeeInvoiceStatus {
    // 인보이스 발행 완료(미납)
    ISSUED,
    // 인보이스 납부 완료
    PAID,
    // 납부 기한 초과
    OVERDUE
}
