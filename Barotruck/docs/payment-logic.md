# Payment 도메인 로직 상세 설명 (현재 코드 기준)

## 0. 문서 목적
이 문서는 `payment` 패키지에서 실제로 어떤 데이터가 어떻게 바뀌는지, API 호출 시 어떤 검증/상태 전이가 일어나는지를 코드 기준으로 정리한 문서다.

기준 코드:
- `src/main/java/com/example/project/domain/payment/controller/PaymentController.java`
- `src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java`
- `src/main/java/com/example/project/domain/payment/service/core/*`
- `src/main/java/com/example/project/domain/payment/domain/*`
- `src/main/java/com/example/project/domain/payment/port/impl/JpaOrderPort.java`
- `src/main/java/com/example/project/domain/settlement/domain/Settlement.java`

---

## 1. 전체 구조

`TransportPaymentService`는 파사드 역할이고, 실제 로직은 아래 3개로 분리되어 있다.

1. `PaymentLifecycleService`
- 수동 결제(`mark-paid`), 외부 결제(`external-pay`), 차주 확정(`confirm`)
- 게이트웨이 승인건을 내부 결제 상태로 반영(`applyPaidFromGatewayTx`)

2. `TossPaymentService`
- 토스 `prepare`, `confirm`, `webhook` 처리
- PG 원장(`PaymentGatewayTransaction`)과 내부 결제(`TransportPayment`)를 연결

3. `PaymentDisputeService`
- 분쟁 생성/상태 변경
- 분쟁 상태를 결제/주문/정산 상태에 동기화

보조 배치:
- `FeeInvoiceBatchService`: 화주 수수료 인보이스 생성/자동청구
- `DriverPayoutService`: 차주 지급 배치/재시도
- `PaymentRetryQueueService`: 토스 실패 재시도 + prepared 만료 처리
- `PaymentReconciliationService`: 토스 원장 vs 내부 원장 대사

---

## 2. 핵심 엔티티와 소스 오브 트루스

## 2.1 TransportPayment
주문당 결제 1건 원칙(`ORDER_ID` unique).

핵심 컬럼:
- `orderId`, `shipperUserId`, `driverUserId`
- `amount` (청구액)
- `feeRateSnapshot`, `feeAmountSnapshot`, `netAmountSnapshot` (정산 스냅샷)
- `method`, `paymentTiming`, `status`
- `pgTid`, `proofUrl`, `paidAt`, `confirmedAt`

상태:
- `READY`, `PAID`, `CONFIRMED`, `DISPUTED`
- `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED`, `CANCELLED`

## 2.2 PaymentGatewayTransaction
토스 결제 세션/승인/실패 재시도용 PG 원장.

핵심 컬럼:
- `provider`, `method`, `payChannel`
- `pgOrderId`, `paymentKey`, `transactionId`
- `amount`, `status`, `expiresAt`, `approvedAt`
- `retryCount`, `nextRetryAt`, `failCode`, `failMessage`, `rawPayload`

상태:
- `PREPARED`, `CONFIRMED`, `FAILED`, `CANCELED`

주의:
- 코드의 JPA unique는 `(PROVIDER, PG_ORDER_ID)`
- DB에 과거 unique `(PROVIDER, PAYMENT_KEY)`가 남아있으면 `paymentKey=null` 중복 오류가 날 수 있음

## 2.3 PaymentDispute
주문당 분쟁 1건(`ORDER_ID` unique).

핵심 컬럼:
- `requesterUserId` (실제 이의 당사자)
- `createdByUserId` (실제 생성자: 차주 본인 또는 관리자)
- `reasonCode`, `description`, `attachmentUrl`
- `status`, `adminMemo`, `requestedAt`, `processedAt`

상태:
- `PENDING`, `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED`

## 2.4 Settlement
결제 흐름에서 문자열 상태를 사용한다.
- `READY`, `COMPLETED`, `DISPUTED`, `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED` 등

---

## 3. 권한 모델

## 3.1 URL 레벨
`SecurityConfiguration`에서 `/api/admin/**`는 `hasRole('ADMIN')`로 보호됨.

## 3.2 메서드 레벨 (`@PreAuthorize`)
주요 결제 API:
- `hasRole('SHIPPER')`: `mark-paid`, `external-pay`, `toss/prepare`, `toss/confirm`, 인보이스 조회/납부
- `hasRole('DRIVER')`: `/orders/{orderId}/confirm`
- `hasAnyRole('DRIVER','ADMIN')`: `/orders/{orderId}/disputes`
- webhook: 인증 애노테이션 없음(수신 전용)

## 3.3 서비스 내부 추가 검증
- Toss prepare/confirm은 "본인 화주 주문" 검증까지 수행
- 분쟁은 DRIVER/ADMIN별 requester 검증 강함
- `mark-paid`, `external-pay`, `confirmByDriver`는 역할 검증은 있으나 "본인 주문/본인 배차" 검증이 toss 대비 약함

---

## 4. 사용자 API 로직 (`/api/v1/payments`)

## 4.1 `POST /orders/{orderId}/toss/prepare`
입력: `method`, `payChannel`, `successUrl`, `failUrl`

동작:
1. 화주 인증 + 본인 주문 검증
2. 주문 snapshot 금액 계산값으로 `amount` 확정
3. 토스 결제수단 보정
- `payChannel=APP_CARD|CARD` -> `method=CARD`
- `payChannel=TRANSFER` -> `method=TRANSFER`
- `method=CASH`는 차단
4. `PaymentGatewayTransaction(PREPARED)` 생성
- `pgOrderId = TOSS-{orderId}-{timestamp}`
- `expiresAt = now + 30분`
5. `provider/pgOrderId/amount/successUrl/failUrl/expiresAt` 반환

상태 변화:
- PG: `PREPARED`
- 결제/주문/정산: 변화 없음

## 4.2 `POST /orders/{orderId}/toss/confirm`
입력: `paymentKey`, `pgOrderId`, `amount`

동작:
1. 화주 인증 + 필수값 검증
2. `pgOrderId`로 PREPARED tx 조회
3. 만료 시 tx를 `FAILED(TX_EXPIRED)` 처리 후 실패 반환
4. `orderId`, `shipperUserId`, `amount` 일치 검증
5. `paymentKey` 중복 사용 검증
6. 토스 confirm API 호출 (`TossPaymentHttpClient`)
7. 성공 시 tx를 `CONFIRMED`로 저장
8. `PaymentLifecycleService.applyPaidFromGatewayTx(tx)` 호출

`applyPaidFromGatewayTx` 반영:
- 결제 없으면 생성, 있으면 재사용
- `paymentTiming`은 게이트웨이 결제라 `POSTPAID` 고정
- 이미 `PAID/CONFIRMED/ADMIN_FORCE_CONFIRMED/DISPUTED/ADMIN_HOLD/ADMIN_REJECTED`면 조기 반환(중복 반영 방지)
- 신규 반영 시:
  - `TransportPayment = PAID`
  - `Order = PAID`
  - `Settlement` upsert 후 `READY`

## 4.3 `POST /orders/{orderId}/confirm` (차주 확정)
동작:
1. DRIVER 역할 검증
2. `TransportPayment` 조회
3. 상태가 `PAID`가 아니면 실패
4. 확정 처리

상태 변화:
- `TransportPayment = CONFIRMED`
- `Order = CONFIRMED`
- `Settlement = COMPLETED`, `feeCompleteDate=now`

## 4.4 `POST /orders/{orderId}/disputes`
동작:
- DRIVER 또는 ADMIN이 분쟁 생성
- 내부적으로 `PaymentDisputeService.createDispute`

생성 규칙:
- DRIVER: 본인 배차건만 가능, `requesterUserId` 대리지정 불가
- ADMIN: `requesterUserId` 필수, 반드시 해당 주문 배차 차주와 일치
- 주문당 기존 분쟁이 있으면 기존 분쟁 반환

상태 변화:
- `PaymentDispute = PENDING`
- `TransportPayment = DISPUTED`
- `Order = DISPUTED` (현재 주문상태가 `COMPLETED`면 건너뜀)
- `Settlement = DISPUTED`

## 4.5 `POST /orders/{orderId}/dispute` (레거시)
- 레거시 경로
- 내부적으로 `PaymentDisputeService.legacyDispute` 위임
- reason `OTHER`, description `legacy dispute request`로 생성

## 4.6 `POST /orders/{orderId}/mark-paid` (수동)
- 화주가 결제 완료를 수동 반영
- 수수료 스냅샷 계산 후 `TransportPayment` 생성/갱신

상태 변화:
- `TransportPayment = PAID`
- `Order = PAID`
- `Settlement = READY` (upsert)

## 4.7 `POST /orders/{orderId}/external-pay` (외부 결제 클라이언트)
- `ExternalPaymentClient` 호출 후 성공이면 paid 반영
- 현재 기본 구현체는 `DummyExternalPaymentClient`(항상 성공)

상태 변화:
- `TransportPayment = PAID`
- `Order = PAID`
- `Settlement = READY`

## 4.8 `POST /webhooks/toss`
동작:
1. `externalEventId` 기준 멱등 처리
2. payload 파싱 후 `paymentKey/pgOrderId/status` 추출
3. tx 조회 후 상태 반영
- canceled -> `CANCELED`
- failed -> `FAILED`
- confirmed 류 -> `CONFIRMED` + `applyPaidFromGatewayTx`

특징:
- confirm API와 webhook 순서가 바뀌어도 최종 수렴하도록 구성

## 4.9 인보이스 사용자 API
- `GET /fee-invoices/me?period=YYYY-MM`
- `POST /fee-invoices/{invoiceId}/mark-paid`

`generateForShipper` 규칙:
- 해당 월 `paidAt` 기준으로 집계
- `PaymentMethod.CARD`는 화주 수수료 인보이스 집계 제외
- 상태가 `PAID` 또는 `CONFIRMED`인 결제만 집계

---

## 5. 관리자 API 로직 (`/api/admin/payment`)

## 5.1 분쟁
- `POST /orders/{orderId}/disputes`
- `PATCH /orders/{orderId}/disputes/{disputeId}/status`

상태 변경 규칙:
- `PENDING`으로 되돌리기 금지
- 매핑:
  - `ADMIN_HOLD` -> 결제 `ADMIN_HOLD`
  - `ADMIN_FORCE_CONFIRMED` -> 결제 `ADMIN_FORCE_CONFIRMED` (+ confirmedAt 없으면 now)
  - `ADMIN_REJECTED` -> 결제 `ADMIN_REJECTED`
- 주문:
  - `ADMIN_FORCE_CONFIRMED`면 `CONFIRMED`
  - 그 외 `DISPUTED`
  - 단, 주문이 이미 `COMPLETED`면 변경 생략
- 정산:
  - `Settlement.status = 분쟁상태명`
  - `ADMIN_FORCE_CONFIRMED`면 `feeCompleteDate=now`

## 5.2 수수료 인보이스 배치
- `POST /fee-invoices/run?period=YYYY-MM`
- `POST /fee-invoices/generate?shipperUserId=...&period=...`

자동 배치:
- 생성 배치 cron: `payment.fee-invoice.generate.cron` (기본 매월 1일 01:05)
- 자동청구 cron: `payment.fee-invoice.auto-charge.cron` (기본 매일 01:20)

## 5.3 차주 지급 배치
- `POST /payouts/run?date=YYYY-MM-DD`
- `POST /payout-items/{itemId}/retry`

대상 규칙:
- `TransportPayment.status in (CONFIRMED, ADMIN_FORCE_CONFIRMED)`
- `confirmedAt <= 지급일`
- 주문당 payout item 1건(unique)

지급 성공 시:
- `DriverPayoutItem = COMPLETED`
- `Settlement = COMPLETED` 자동 반영

## 5.4 운영 복구성 API
- `POST /toss/expire-prepared/run`: 만료된 PREPARED를 FAILED(EXPIRED) 처리
- `POST /toss/retries/run`: retry 가능한 FAILED tx 재확인
- `POST /reconciliation/run`: CONFIRMED tx 중 내부 미반영건 복구 시도

---

## 6. 상태 전이 요약

| 트리거 | GatewayTx | TransportPayment | Order | PaymentDispute | Settlement |
|---|---|---|---|---|---|
| toss prepare | PREPARED | 변화 없음 | 변화 없음 | 변화 없음 | 변화 없음 |
| toss confirm 성공 | CONFIRMED | PAID | PAID | - | READY |
| driver confirm | 변화 없음 | CONFIRMED | CONFIRMED | - | COMPLETED |
| 분쟁 생성 | 변화 없음 | DISPUTED | DISPUTED* | PENDING | DISPUTED |
| 분쟁 -> ADMIN_HOLD | 변화 없음 | ADMIN_HOLD | DISPUTED* | ADMIN_HOLD | ADMIN_HOLD |
| 분쟁 -> ADMIN_FORCE_CONFIRMED | 변화 없음 | ADMIN_FORCE_CONFIRMED | CONFIRMED* | ADMIN_FORCE_CONFIRMED | ADMIN_FORCE_CONFIRMED (+completeDate) |
| 분쟁 -> ADMIN_REJECTED | 변화 없음 | ADMIN_REJECTED | DISPUTED* | ADMIN_REJECTED | ADMIN_REJECTED |
| payout 성공 | 변화 없음 | 변화 없음 | 변화 없음 | 변화 없음 | COMPLETED |

`*` 주문 상태가 이미 `COMPLETED`면 결제 플로우에서 변경하지 않음.

---

## 7. 토스 연동에서 프론트/백엔드 경계

## 7.1 프론트 책임
1. `toss/prepare` 호출로 `pgOrderId`, `amount` 수신
2. Toss SDK 결제창 호출 (`orderId=pgOrderId`, `amount` 사용)
3. 성공 콜백에서 `paymentKey` 획득
4. `toss/confirm` 호출

## 7.2 백엔드 책임
- prepare 세션 발급, 금액/소유권/만료/중복 검증
- 토스 confirm API 서버-서버 호출
- 내부 결제/정산 상태 전이
- webhook 멱등 수신 및 지연 반영
- 재시도/대사 배치로 최종 정합성 복구

---

## 8. 현재 구현체 기준 운영 주의사항

1. 더미 구현체 존재
- `DummyExternalPaymentClient`
- `DummyDriverPayoutGatewayClient`
- `DummyFeeAutoChargeClient`

2. 토스 confirm 키 설정
- `payment.toss.secret-key` 미설정 시
  - `payment.toss.mock-confirm-enabled=true`면 mock confirm 동작
  - 아니면 confirm 실패

3. paymentKey unique 이슈 가능성
- 코드상 unique는 `(provider, pgOrderId)`
- DB에 과거 제약 `(provider, payment_key)`가 남아 있으면 prepare 시점 null 충돌 가능

4. 테스트 페이지
- `/api/v1/payments/toss-test`
- `/api/v1/payments/api-test`
- `/api/v1/payments/admin-test`
- `/api/v1/payments/admin-cycles` 및 `/admin-cycle-a` ~ `/admin-cycle-f`

---

## 9. 요약

현재 payment 도메인은 아래를 이미 제공한다.
- 토스 prepare/confirm/webhook 기반 결제 반영
- 분쟁 생성/관리자 처리 및 상태 동기화
- 화주 수수료 인보이스 생성/납부
- 차주 지급 배치/재시도
- 재시도 큐 + 대사 배치로 장애 복구

핵심은 `PaymentGatewayTransaction + TransportPayment + Settlement` 3축을 일관되게 맞추는 구조다.
