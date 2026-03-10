package com.example.project.domain.payment.domain.paymentEnum;

public final class PaymentEnums {

    private PaymentEnums() {
    }

    public enum FeeInvoiceStatus {
        // 인보이스 발행됨
        ISSUED,
        // 인보이스 납부 완료
        PAID,
        // 납부 기한 초과
        OVERDUE
    }

    public enum GatewayTxStatus {
        // PG 결제 준비 완료
        PREPARED,
        // PG 승인 완료
        CONFIRMED,
        // PG 승인/처리 실패
        FAILED,
        // PG 취소
        CANCELED
    }

    public enum BillingAgreementStatus {
        // 사용 가능
        ACTIVE,
        // 해지/비활성
        INACTIVE,
        // 외부 삭제/만료
        DELETED
    }

    public enum FeeAutoChargeStatus {
        // 자동청구 성공
        SUCCEEDED,
        // 자동청구 실패
        FAILED
    }

    public enum PayChannel {
        // 일반 카드 결제
        CARD,
        // 앱카드/간편결제 카드
        APP_CARD,
        // 계좌이체
        TRANSFER
    }

    public enum PaymentDisputeReason {
        // 청구 금액 불일치
        PRICE_MISMATCH,
        // 수령 금액 불일치
        RECEIVED_AMOUNT_MISMATCH,
        // 증빙 누락
        PROOF_MISSING,
        // 사기 의심
        FRAUD_SUSPECTED,
        // 기타
        OTHER
    }

    public enum PaymentDisputeStatus {
        // 접수됨(처리 전)
        PENDING,
        // 관리자 보류
        ADMIN_HOLD,
        // 관리자 강제확정
        ADMIN_FORCE_CONFIRMED,
        // 관리자 반려
        ADMIN_REJECTED
    }

    public enum PaymentMethod {
        // 카드
        CARD,
        // 계좌이체
        TRANSFER,
        // 현금
        CASH
    }

    public enum PaymentProvider {
        // 토스
        TOSS
    }

    public enum PaymentTiming {
        // 선불
        PREPAID,
        // 후불
        POSTPAID
    }

    public enum PayoutStatus {
        // 지급 준비
        READY,
        // 지급 요청됨
        REQUESTED,
        // 지급 완료
        COMPLETED,
        // 지급 실패
        FAILED,
        // 재시도 중
        RETRYING
    }

    public enum TransportPaymentStatus {
        // 결제 준비
        READY,
        // 결제 완료
        PAID,
        // 차주 확인 완료
        CONFIRMED,
        // 이의제기 상태
        DISPUTED,
        // 관리자 보류
        ADMIN_HOLD,
        // 관리자 강제확정
        ADMIN_FORCE_CONFIRMED,
        // 관리자 반려
        ADMIN_REJECTED,
        // 결제 취소
        CANCELLED
    }
}
