# Payment 도메인 로직 상세 설명

기준일: 2026-03-10

이 문서는 현재 백엔드 코드 기준으로 결제, billing, payout 로직이 실제로 어떻게 동작하는지 정리한 문서다.

문서 우선순위:

1. 이 문서
2. `docs/toss-payment-project-summary.md`
3. `docs/toss-payment-enhancement-plan.md`
4. `docs/toss-payment-implementation-handoff.md`
5. `docs/toss-payment-gap-review.md`

---

## 1. 전체 구조

현재 payment 패키지는 아래 역할로 나뉜다.

1. `TransportPaymentService`
- 결제 파사드
- 사용자/관리자 컨트롤러에서 호출하는 진입점

2. `PaymentLifecycleService`
- `mark-paid`, driver confirm, gateway paid 반영, gateway cancel 반영
- `TransportPayment`, `Order`, `Settlement` 상태 전이의 핵심

3. `TossPaymentService`
- Toss `prepare`, `confirm`, `webhook`
- `PaymentGatewayTransaction`과 내부 결제 상태를 연결

4. `TossPaymentOpsService`
- 관리자용 Toss 실조회와 cancel
- 내부 원장과 Toss 응답 비교

5. `ShipperBillingAgreementService`
- billing context
- billing key 발급, 조회, 해지

6. `AdminPaymentStatusQueryService`
- dispute, fee invoice, billing agreement, auto-charge attempt, payout, retry, reconciliation 상태 조회

보조 배치와 비동기 흐름:

- `FeeInvoiceBatchService`: 인보이스 생성과 자동청구
- `DriverPayoutService`: 차주 지급 배치, 재시도, polling sync
- `TossPayoutWebhookService`: `payout.changed`, `seller.changed` 반영
- `PaymentRetryQueueService`: failed confirm 재시도, expired prepared 정리
- `PaymentReconciliationService`: confirmed gateway tx와 내부 결제 대사

---

## 2. 핵심 엔티티와 소스 오브 트루스

### 2.1 `TransportPayment`

주문당 결제 1건 기준의 내부 결제 원장이다.

핵심 필드:

- `orderId`, `shipperUserId`, `driverUserId`
- `amount`
- `feeRateSnapshot`, `feeAmountSnapshot`, `netAmountSnapshot`
- `method`, `paymentTiming`, `status`
- `pgTid`, `proofUrl`, `paidAt`, `confirmedAt`

주요 상태:

- `READY`
- `PAID`
- `CONFIRMED`
- `DISPUTED`
- `ADMIN_HOLD`
- `ADMIN_FORCE_CONFIRMED`
- `ADMIN_REJECTED`
- `CANCELLED`

### 2.2 `PaymentGatewayTransaction`

Toss 결제 준비, 승인, 실패, 취소를 저장하는 PG 원장이다.

핵심 필드:

- `provider`, `method`, `payChannel`
- `pgOrderId`, `paymentKey`, `transactionId`
- `amount`, `status`, `gatewayStatus`
- `expiresAt`, `approvedAt`
- `cancelReason`, `canceledAmount`, `canceledAt`, `cancelTransactionId`
- `retryCount`, `nextRetryAt`
- `failCode`, `failMessage`, `rawPayload`

주요 상태:

- `PREPARED`
- `CONFIRMED`
- `FAILED`
- `CANCELED`

고유 제약:

- 코드 기준 unique는 `(PROVIDER, PG_ORDER_ID)`다.

### 2.3 `ShipperBillingAgreement`

화주별 billing key 상태를 저장한다.

핵심 필드:

- `shipperUserId`
- `customerKey`
- `billingKey`
- `status`
- `cardCompany`, `cardNumberMasked`, `cardType`, `ownerType`
- `authenticatedAt`, `lastChargedAt`, `deactivatedAt`

주요 상태:

- `ACTIVE`
- `INACTIVE`
- `DELETED`

### 2.4 `FeeAutoChargeAttempt`

수수료 인보이스 자동청구 시도 원장이다.

핵심 필드:

- `invoiceId`, `shipperUserId`, `agreementId`
- `orderId`, `paymentKey`, `transactionId`
- `amount`
- `status`
- `failCode`, `failReason`, `rawPayload`
- `attemptedAt`

주요 상태:

- `SUCCEEDED`
- `FAILED`

### 2.5 `PaymentGatewayWebhookEvent`

PG 웹훅 멱등 처리와 운영 추적을 위한 이벤트 원장이다.

핵심 필드:

- `externalEventId`
- `eventType`
- `payload`
- `receivedAt`
- `processedAt`
- `processResult`

### 2.6 `Settlement`

정산 도메인의 실제 상태 원장이다.

현재 결제 흐름에서 주로 쓰는 상태:

- `READY`
- `COMPLETED`
- `DISPUTED`
- `ADMIN_HOLD`
- `ADMIN_FORCE_CONFIRMED`
- `ADMIN_REJECTED`

---

## 3. 사용자 API 로직 (`/api/v1/payments`)

### 3.1 `POST /orders/{orderId}/mark-paid`

수동 결제 반영 API다.

동작:

1. SHIPPER 인증 확인
2. 본인 주문 여부 검증
3. 운송 완료 이후 상태인지 검증
4. 필요 시 `TransportPayment`를 생성하거나 재사용
5. 수수료 스냅샷 계산 후 내부 결제를 `PAID`로 반영
6. `Order = PAID`, `Settlement = READY`

주의:

- `TRANSFER` 수동 결제는 `proofUrl`이 필요하다.
- 수동 결제의 기본 `paymentTiming`은 `PREPAID`다.

### 3.2 `POST /orders/{orderId}/confirm`

차주 최종 확인 API다.

동작:

1. DRIVER 인증 확인
2. `TransportPayment` 조회
3. 본인 배차 여부 검증
4. 결제가 `PAID`일 때만 확정
5. `TransportPayment = CONFIRMED`
6. `Order = CONFIRMED`
7. `Settlement = COMPLETED`

### 3.3 `POST /orders/{orderId}/disputes`

DRIVER 또는 ADMIN이 분쟁을 생성한다.

반영:

- `PaymentDispute = PENDING`
- `TransportPayment = DISPUTED`
- `Order = DISPUTED` 단, 이미 완료된 주문은 유지 가능
- `Settlement = DISPUTED`

### 3.4 `POST /orders/{orderId}/toss/prepare`

Toss 결제 세션 준비 API다.

동작:

1. SHIPPER 인증과 본인 주문 검증
2. 운송 완료 이후 상태인지 검증
3. `client-key` 설정 여부 확인
4. 수수료 정책 계산 후 Toss 청구 금액 산정
5. 추가로 `10%` surcharge를 더해 `PaymentGatewayTransaction(PREPARED)` 생성
6. `pgOrderId`, `amount`, `successUrl`, `failUrl` 반환

상태 변화:

- `PaymentGatewayTransaction = PREPARED`
- 내부 결제, 주문, 정산 상태는 아직 바뀌지 않음

### 3.5 `POST /orders/{orderId}/toss/confirm`

Toss 승인 반영 API다.

동작:

1. SHIPPER 인증과 본인 주문 검증
2. `pgOrderId` 또는 최신 Toss tx로 준비 거래 조회
3. 만료면 `FAILED(TX_EXPIRED)` 처리
4. `amount`, `paymentKey` 중복 여부 검증
5. Toss confirm 호출
6. 성공 시 tx를 `CONFIRMED`로 저장
7. `PaymentLifecycleService.applyPaidFromGatewayTx(tx)` 호출

`applyPaidFromGatewayTx` 결과:

- `TransportPayment = PAID`
- `Order = PAID`
- `Settlement = READY`

특징:

- 이미 같은 tx가 `CONFIRMED`면 중복 반영 대신 기존 결제 상태를 재적용한다.
- gateway 결제의 `paymentTiming`은 `POSTPAID`로 고정된다.

### 3.6 `POST /webhooks/toss`

Toss 웹훅 수신 API다.

공통 동작:

1. `Toss-Event-Id` 또는 대체 ID로 멱등 처리
2. `PaymentGatewayWebhookEvent` 저장
3. 설정된 경우 `payment.toss.webhook-secret` 검증
4. event type에 따라 payment 또는 payout 흐름으로 분기

payment 이벤트:

- `paymentKey` 또는 `pgOrderId`로 tx 탐색
- canceled 계열이면 tx를 `CANCELED`로 반영
- failed 계열이면 tx를 `FAILED`로 반영
- confirmed 계열이면 tx를 `CONFIRMED`로 맞추고 내부 결제 paid 반영

payout/seller 이벤트:

- `payout.changed`
- `seller.changed`

이 경우 `TossPayoutWebhookService`가 호출된다.

### 3.7 `GET /billing/context`

billing 등록 화면이 필요한 컨텍스트를 내려준다.

반환:

- `clientKey`
- `customerKey`
- billing success URL
- billing fail URL

### 3.8 `POST /billing/agreements`

billing key 발급 API다.

동작:

1. SHIPPER 인증 확인
2. `authKey` 필수값 검증
3. `customerKey` 일치 검증
4. Toss `issueBillingKey` 호출
5. 기존 ACTIVE agreement가 있으면 비활성화
6. 새 agreement 저장 또는 기존 agreement 갱신

### 3.9 `GET /billing/agreements/me`

화주의 최신 billing agreement 조회 API다.

### 3.10 `DELETE /billing/agreements/me`

화주의 billing agreement 해지 API다.

동작:

1. ACTIVE agreement 조회
2. Toss `deleteBillingKey` 호출
3. agreement 상태를 `INACTIVE`로 변경

### 3.11 인보이스 사용자 API

현재 사용자 API:

- `GET /fee-invoices/me?period=YYYY-MM`
- `POST /fee-invoices/{invoiceId}/mark-paid`

자동청구와 별개로 수동 납부 반영도 가능하다.

---

## 4. 관리자 API 로직 (`/api/admin/payment`)

### 4.1 분쟁과 결제 상태 관리

현재 관리자 API:

- `PATCH /orders/{orderId}/status`
- `POST /orders/{orderId}/disputes`
- `PATCH /orders/{orderId}/disputes/{disputeId}/status`
- `GET /orders/{orderId}/disputes/status`

분쟁 상태 반영:

- `ADMIN_HOLD` -> 결제 `ADMIN_HOLD`
- `ADMIN_FORCE_CONFIRMED` -> 결제 `ADMIN_FORCE_CONFIRMED`
- `ADMIN_REJECTED` -> 결제 `ADMIN_REJECTED`

### 4.2 fee invoice와 billing 운영 조회

현재 관리자 API:

- `POST /fee-invoices/run`
- `POST /fee-invoices/generate`
- `GET /fee-invoices/status`
- `GET /billing-agreements/status`
- `GET /fee-auto-charge-attempts`

현재 조회 가능한 것:

- 화주별 fee invoice 상태
- 최신 billing agreement
- 최근 auto-charge attempt 목록
- 성공/실패 누적 건수

### 4.3 payout 운영

현재 관리자 API:

- `POST /payouts/run`
- `GET /payouts/status`
- `POST /payout-items/{itemId}/retry`
- `GET /payout-items/orders/{orderId}/status`

`payout-items/orders/{orderId}/status` 응답에는 아래 운영 정보가 포함된다.

- payout item 상태
- seller id, seller ref, seller status
- 최신 webhook id, event type, 처리 결과
- webhook status와 payout status 일치 여부

### 4.4 Toss 운영 복구성

현재 관리자 API:

- `POST /toss/expire-prepared/run`
- `GET /toss/expire-prepared/status`
- `POST /toss/retries/run`
- `GET /toss/retries/status`
- `POST /reconciliation/run`
- `GET /reconciliation/status`

현재 이미 운영 가능한 것:

- 만료된 prepared 정리
- retry 가능한 failed confirm 재시도
- confirmed tx와 내부 결제 불일치 복구 시도

### 4.5 Toss 실조회와 cancel

현재 관리자 API:

- `GET /toss/orders/{orderId}/status`
- `GET /toss/payments/{paymentKey}`
- `GET /toss/orders/{orderId}/lookup`
- `POST /orders/{orderId}/cancel`

lookup:

- `paymentKey` 기준 Toss 실조회
- `orderId` 기준 최신 tx를 찾은 뒤 내부 결제와 Toss 결과 비교

cancel:

- confirmed Toss tx만 대상
- 내부 결제가 `PAID`일 때만 취소
- payout item이 이미 있으면 취소 금지
- 부분취소는 현재 미지원

cancel 반영 시 주의:

- `TransportPayment`와 `Settlement`는 갱신될 수 있다.
- 현재 cancel 로직은 `Order` 상태를 직접 되돌리지 않는다.

---

## 5. 배치와 비동기 동작

### 5.1 fee invoice 생성과 자동청구

`FeeInvoiceBatchService`는 아래 두 가지를 수행한다.

1. 전월 인보이스 생성
2. `ISSUED`, `OVERDUE` 인보이스 자동청구

자동청구 동작:

1. `FeeAutoChargeClient.charge(...)`
2. 성공이면 invoice `PAID`
3. 실패면 invoice `OVERDUE`
4. 성공/실패 모두 `FeeAutoChargeAttempt` 저장

현재 구현체 선택:

- 기본값: `TossFeeAutoChargeClient`
- `payment.fee-auto-charge.mock-enabled=true`일 때만 `DummyFeeAutoChargeClient`

### 5.2 차주 지급 배치

`DriverPayoutService`는 아래 흐름을 수행한다.

1. 지급 대상 `TransportPayment(CONFIRMED, ADMIN_FORCE_CONFIRMED)` 선별
2. `DriverPayoutItem` 생성
3. Toss payout 요청
4. 완료면 `Settlement = COMPLETED`
5. 요청 후 완료 전 상태는 polling과 webhook로 동기화

현재 구현체 선택:

- 기본값: `TossDriverPayoutGatewayClient`
- `payment.payout.mock-enabled=true`일 때만 dummy

### 5.3 payout webhook 동기화

`TossPayoutWebhookService`는 아래를 수행한다.

- payout 상태를 `DriverPayoutItem`에 반영
- 완료면 `Settlement = COMPLETED`
- seller 상태를 `Driver.tossPayoutSellerStatus`에 반영
- batch 상태를 재계산

### 5.4 retry queue와 reconciliation

`PaymentRetryQueueService`:

- 만료된 `PREPARED`를 `FAILED`로 전환
- retry 가능한 `FAILED` tx를 다시 confirm

`PaymentReconciliationService`:

- `CONFIRMED` tx인데 내부 결제가 없는 건을 다시 paid 반영

---

## 6. 상태 전이 요약

| 트리거 | GatewayTx | TransportPayment | Order | PaymentDispute | Settlement |
|---|---|---|---|---|---|
| toss prepare | PREPARED | 변화 없음 | 변화 없음 | 변화 없음 | 변화 없음 |
| toss confirm 성공 | CONFIRMED | PAID | PAID | - | READY |
| payment webhook confirmed | CONFIRMED | PAID | PAID | - | READY |
| driver confirm | 변화 없음 | CONFIRMED | CONFIRMED | - | COMPLETED |
| 분쟁 생성 | 변화 없음 | DISPUTED | DISPUTED 또는 유지 | PENDING | DISPUTED |
| 분쟁 -> ADMIN_HOLD | 변화 없음 | ADMIN_HOLD | DISPUTED 또는 유지 | ADMIN_HOLD | ADMIN_HOLD |
| 분쟁 -> ADMIN_FORCE_CONFIRMED | 변화 없음 | ADMIN_FORCE_CONFIRMED | CONFIRMED 또는 유지 | ADMIN_FORCE_CONFIRMED | ADMIN_FORCE_CONFIRMED |
| 분쟁 -> ADMIN_REJECTED | 변화 없음 | ADMIN_REJECTED | DISPUTED 또는 유지 | ADMIN_REJECTED | ADMIN_REJECTED |
| admin cancel 또는 cancel webhook | CANCELED | CANCELLED 조건부 반영 | 직접 변경 없음 | - | READY 조건부 반영 |
| payout 완료 | 변화 없음 | 변화 없음 | 변화 없음 | - | COMPLETED |

cancel 조건부 반영:

- 내부 결제가 `READY` 또는 `PAID`
- payout item이 아직 없음

---

## 7. 운영 주의사항

### 7.1 필수 설정

- `payment.toss.client-key`
- `payment.toss.secret-key`
- `payment.toss.billing.secret-key`
- `payment.toss.payout.secret-key`
- `payment.toss.payout.security-key`

없으면 해당 실호출은 실패한다.

### 7.2 mock 토글

기본값은 둘 다 `false`다.

- `payment.fee-auto-charge.mock-enabled=false`
- `payment.payout.mock-enabled=false`

즉, 설정만 보면 기본 동작은 더미가 아니라 실제 Toss client 쪽이다.

### 7.3 webhook secret

`payment.toss.webhook-secret`가 설정돼 있으면 header 또는 payload의 secret과 비교한다.
설정이 비어 있으면 secret 검증은 생략된다.

### 7.4 이 문서가 다루지 않는 것

이 문서는 백엔드 코드 기준이다.
관리자 웹과 앱에서 실제 어떤 화면이 연결됐는지는 별도 프론트 저장소 또는 운영 문서에서 확인해야 한다.

---

## 8. 요약

현재 payment 도메인은 아래까지는 이미 제공한다.

- Toss prepare/confirm/webhook 기반 결제 반영
- 관리자 Toss lookup/cancel
- billing key 발급/조회/해지
- 실제 auto-charge client와 시도 원장
- payout 요청, polling, webhook 동기화
- retry queue와 reconciliation

지금 남은 핵심은 구현 부재보다 `런타임 검증`, `운영 화면 반영`, `정책 마감`이다.
