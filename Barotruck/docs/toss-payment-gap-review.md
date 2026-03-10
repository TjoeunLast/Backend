# Toss 결제 문서 정합성 리뷰

기준일: 2026-03-10

이 문서는 Toss 결제 관련 문서를 현재 백엔드 코드 기준으로 다시 맞춘 뒤, 아직 남아 있는 실제 공백만 정리한 리뷰 문서다.

---

## 1. 결론

이번 정합성 정리 이후 아래 판단이 현재 기준으로 맞다.

1. 문서 간 직접 충돌은 대부분 제거됐다.
2. 예전 문서의 `없음` 표기는 다수 `부분 구현` 또는 `구현됨`으로 수정돼야 했고, 그 작업을 반영했다.
3. 이제 남은 핵심 공백은 문서 자체보다 `런타임 검증`, `세부 정책`, `화면 반영 확인` 쪽이다.

---

## 2. 이번에 바로잡은 핵심 오판

### 2.1 billing은 더 이상 `없음`이 아니다

현재 코드에는 아래가 존재한다.

- billing context API
- billing key 발급 API
- billing agreement 조회/해지 API
- `ShipperBillingAgreement` 원장
- `TossFeeAutoChargeClient`
- `FeeAutoChargeAttempt` 원장
- 관리자 billing agreement status 조회
- 관리자 auto-charge attempt 조회

정확한 표현:

- `없음`이 아니라 `부분 구현`

### 2.2 Toss lookup/cancel은 더 이상 `없음`이 아니다

현재 코드에는 아래가 존재한다.

- Toss lookup HTTP client
- 관리자 `paymentKey` lookup API
- 관리자 `orderId` lookup API
- 관리자 cancel API
- cancel 후 내부 결제 취소 반영 로직

정확한 표현:

- `없음`이 아니라 `부분 구현`

### 2.3 payout webhook은 더 이상 `없음`이 아니다

현재 코드에는 아래가 존재한다.

- `/api/v1/payments/webhooks/toss` 내부 payout/seller 라우팅
- `TossPayoutWebhookService`
- `payout.changed`, `seller.changed` 처리
- 관리자 payout item status에서 최신 webhook 확인

정확한 표현:

- `없음`이 아니라 `부분 구현`

---

## 3. 지금도 남아 있는 실제 공백

### 3.1 런타임 검증 미완료

문서를 고쳐도 아래는 아직 남아 있다.

- Toss lookup 실제 호출 검증
- Toss cancel 실제 호출 검증
- billing issue/charge/delete 실제 호출 검증
- payout/seller webhook 샘플 payload 검증

즉, 코드상 존재와 운영 검증 완료는 아직 다르다.

### 3.2 세부 정책 미확정

현재 코드가 의도적으로 좁혀 둔 부분이 있다.

- 부분취소 미지원
- payout item 존재 시 cancel 금지
- billing 실패 재시도 정책 단순
- billing key 저장/운영 정책 추가 정리 필요

### 3.3 UI 연결 상태는 이 저장소만으로 닫히지 않음

현재 문서는 백엔드 기준으로 맞췄다.
하지만 아래는 별도 프론트 저장소 또는 운영 화면 기준 확인이 더 필요하다.

- 관리자 웹의 PG 실조회 패널
- 관리자 웹의 취소/환불 UX
- billing status/auto-charge attempt 운영 화면
- payout webhook 운영 화면
- 화주 앱의 billing 등록/조회/해지 화면

### 3.4 환경 반영 여부는 코드만으로 확정할 수 없음

아래는 코드 문서가 아니라 환경 확인 대상이다.

- Oracle에서 JPA `update` 반영 허용 여부
- 운영에서 `JPA_DDL_AUTO=validate`로 덮어쓸지 여부
- 운영/스테이징 secret 설정 여부

---

## 4. 문서별 현재 역할

### 4.1 `docs/payment-logic.md`

가장 정확한 기준 문서다.
상세 상태 전이와 API 동작은 이 문서를 우선한다.

### 4.2 `docs/toss-payment-project-summary.md`

현재 상태를 빠르게 판단하는 요약본이다.
`구현됨`, `부분 구현`, `운영 미검증`, `없음` 구분은 이 문서가 기준이다.

### 4.3 `docs/toss-payment-enhancement-plan.md`

이미 들어간 구현을 제외하고 남은 작업만 정리한 계획서다.

### 4.4 `docs/toss-payment-implementation-handoff.md`

최근 백엔드 확장 범위와 재개 지점을 보는 문서다.

---

## 5. 추천 읽기 순서

1. `docs/payment-logic.md`
2. `docs/toss-payment-project-summary.md`
3. `docs/toss-payment-enhancement-plan.md`
4. `docs/toss-payment-implementation-handoff.md`
5. `docs/toss-payment-gap-review.md`

---

## 6. 정리

지금 기준에서 문서가 말해야 할 핵심은 단순하다.

1. backend 구현은 예전 문서보다 훨씬 더 진행돼 있다.
2. 다만 운영 검증과 UI 반영은 아직 별도 확인이 필요하다.
3. 따라서 현재 적절한 표현은 `없음`보다 `부분 구현`과 `운영 미검증`이다.
