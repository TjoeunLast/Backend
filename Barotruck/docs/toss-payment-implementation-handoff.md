# Toss 결제 보강 인수인계

기준일: 2026-03-10

이 문서는 현재 백엔드 구현 상태를 인수인계하기 위한 스냅샷이다.
동작 규칙의 최종 기준은 `docs/payment-logic.md`, 남은 과제의 우선순위는 `docs/toss-payment-enhancement-plan.md`를 따른다.

현재 확인된 상태:

- `./gradlew.bat compileJava` 통과 확인: 2026-03-10
- 컨트롤러/API 노출은 이미 진행됨
- 실키 및 실웹훅 기준 런타임 검증은 아직 문서로 닫히지 않음

---

## 1. 현재 완료된 백엔드 범위

### 1.1 Toss 결제 클라이언트 확장

현재 `TossPaymentHttpClient`에는 아래 호출이 들어가 있다.

- `confirm`
- `lookupByPaymentKey`
- `lookupByOrderId`
- `cancel`
- `issueBillingKey`
- `chargeBillingKey`
- `deleteBillingKey`

즉, 조회, 취소, billing 호출용 HTTP 클라이언트는 이미 준비된 상태다.

### 1.2 사용자 결제 API

`PaymentController` 기준으로 아래 사용자 API가 노출돼 있다.

- `POST /api/v1/payments/orders/{orderId}/toss/prepare`
- `POST /api/v1/payments/orders/{orderId}/toss/confirm`
- `POST /api/v1/payments/webhooks/toss`
- `GET /api/v1/payments/billing/context`
- `POST /api/v1/payments/billing/agreements`
- `GET /api/v1/payments/billing/agreements/me`
- `DELETE /api/v1/payments/billing/agreements/me`

정리하면, billing은 더 이상 "서비스만 있고 API 미노출" 상태가 아니다.

### 1.3 관리자 운영 API

`AdminPaymentController` 기준으로 아래 운영 API가 노출돼 있다.

- Toss lookup
  - `GET /api/admin/payment/toss/payments/{paymentKey}`
  - `GET /api/admin/payment/toss/orders/{orderId}/lookup`
- Toss cancel
  - `POST /api/admin/payment/orders/{orderId}/cancel`
- billing 운영 조회
  - `GET /api/admin/payment/billing-agreements/status`
  - `GET /api/admin/payment/fee-auto-charge-attempts`
- payout 운영 조회
  - `GET /api/admin/payment/payouts/status`
  - `GET /api/admin/payment/payout-items/orders/{orderId}/status`
- 복구성 배치
  - expire prepared
  - retry queue
  - reconciliation

즉, lookup/cancel과 billing status도 더 이상 "컨트롤러 미노출"이 아니다.

### 1.4 billing agreement와 auto-charge 원장

현재 코드에는 아래 엔티티와 저장 흐름이 있다.

- `ShipperBillingAgreement`
- `FeeAutoChargeAttempt`
- `ShipperBillingAgreementService`
- `TossFeeAutoChargeClient`
- `FeeInvoiceBatchService`

현재 동작:

- 화주가 billing key를 발급, 조회, 해지할 수 있다.
- 수수료 인보이스 자동청구는 기본값 기준 실제 Toss client를 사용한다.
- 자동청구 성공/실패 시도는 `FeeAutoChargeAttempt`에 저장된다.

### 1.5 payout webhook 반영

현재 `/api/v1/payments/webhooks/toss` 내부에서 아래 이벤트를 라우팅한다.

- `payout.changed`
- `seller.changed`

현재 반영 범위:

- `DriverPayoutItem` 상태 동기화
- `Settlement` 완료 반영
- `Driver.tossPayoutSellerStatus` 동기화
- 관리자 payout item status 응답에 최신 webhook 정보 포함

---

## 2. 지금 기준으로 "부분 구현"인 항목

### 2.1 Toss cancel 정책

현재 cancel은 동작하지만 범위가 제한적이다.

- confirmed transaction만 대상
- 내부 결제가 `PAID`일 때만 안전 취소
- payout item이 있으면 취소 금지
- 부분취소 미지원

즉, "실취소 API는 있음"이 맞지만 "환불 정책 전체가 닫힘"은 아직 아니다.

### 2.2 billing/auto-charge 운영

현재 백엔드 API와 원장은 있다.
다만 아래는 아직 남아 있다.

- 화주 UI 반영 여부 미확인
- 관리자 UI 반영 여부 미확인
- 실제 billing issue/charge/delete 검증 미완료
- 미납/재시도 정책 문서화 부족

### 2.3 payout 운영

현재 payout 요청, polling, webhook가 모두 존재한다.
다만 아래는 아직 남아 있다.

- 실 payout 검증
- 실 webhook payload 검증
- seller lifecycle 운영 UI 미확인

---

## 3. 재개 우선순위

### 3.1 1순위: 런타임 검증

아래를 먼저 확인한다.

1. lookup
2. cancel
3. billing issue
4. billing charge
5. billing delete
6. payout webhook
7. seller webhook

### 3.2 2순위: 운영 화면 반영 확인

백엔드 API는 이미 있으므로 다음은 화면 연결 여부를 닫는 단계다.

- 화주 결제 상세의 PG 실조회
- 화주 결제 상세의 취소 UX
- billing status/auto-charge attempt 운영 화면
- payout item과 최신 webhook 반영 화면

### 3.3 3순위: 정책 확정

- 부분취소 허용 여부
- payout 이후 취소 허용 여부
- billing 실패 재시도 정책

---

## 4. 문서 읽기 기준

1. 실제 상태 전이와 API 동작은 `docs/payment-logic.md`를 본다.
2. 남은 작업만 보려면 `docs/toss-payment-enhancement-plan.md`를 본다.
3. 문서 간 정합성 리스크는 `docs/toss-payment-gap-review.md`를 본다.

---

## 5. 핵심 정정 포인트

이번 정합성 갱신에서 특히 바뀐 판단은 아래다.

- `billing API 미노출` -> 잘못된 서술
- `lookup/cancel API 미노출` -> 잘못된 서술
- `payout webhook 없음` -> 잘못된 서술
- `자동청구는 더미만 있음` -> 잘못된 서술

정확한 표현은 아래와 같다.

- billing, lookup, cancel, payout webhook는 `백엔드 부분 구현 이상`
- 아직 남은 것은 `운영 검증`과 `화면/정책 마감`
