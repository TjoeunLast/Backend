# Barotruck Toss 결제 남은 보강 계획

기준일: 2026-03-10

이 문서는 현재 코드에 이미 들어간 항목과 아직 남은 항목을 분리해서, 실제로 남은 보강 과제만 정리한 계획서다.

판단 기준:

- 현재 백엔드 코드 기준
- `docs/payment-logic.md`와 `docs/toss-payment-project-summary.md`를 상위 기준으로 사용

---

## 1. 계획 요약

예전 계획 문서처럼 모든 항목을 `미착수`로 보면 현재 진행률이 왜곡된다.
지금은 아래처럼 보는 것이 맞다.

| 트랙 | 현재 상태 | 이미 들어간 것 | 남은 작업 |
|---|---|---|---|
| Toss 실조회 API | 부분 완료 | HTTP client, 관리자 lookup API, 내부/PG 비교 응답 | 관리자 화면 연결, 실조회 실패 UX 정리, 실호출 검증 |
| Toss 실취소/환불 API | 부분 완료 | HTTP client, 관리자 cancel API, 내부 cancel 반영 | 부분취소 정책, 감사 로그/운영 정책, 실호출 검증 |
| payout webhook 반영 | 부분 완료 | webhook 라우팅, payout/seller 상태 반영, 관리자 payout status 조회 | 실 payload 검증, 운영 화면 반영, seller lifecycle 운영 보강 |
| 수수료 자동청구 실결제 전환 | 부분 완료 | billing agreement 도메인, 사용자 API, 실제 Toss auto-charge client, 시도 원장 | 화주 UI, 관리자 UI, 미납/재시도 정책, 실결제 검증 |
| 관리자 운영 화면 보강 | 백엔드 부분 완료 | status API와 lookup/cancel API가 존재 | 실제 관리자 웹 반영 여부 확인과 UX 마감 |

---

## 2. 최우선 잔여 작업

### 2.1 런타임 검증과 결과 문서화

가장 먼저 닫아야 할 항목이다.

검증 대상:

1. Toss payment lookup
2. Toss payment cancel
3. billing key issue
4. billing key charge
5. billing key delete
6. `payout.changed`
7. `seller.changed`

완료 기준:

- 테스트 키 또는 샘플 payload 기준으로 성공/실패 케이스를 재현한다.
- 어떤 요청값과 응답을 확인했는지 문서에 남긴다.
- 성공 여부만 적지 말고 실패 시 내부 상태가 어떻게 남는지도 확인한다.

### 2.2 운영 조회 정보의 한 화면 정리

백엔드 API는 이미 흩어져 존재한다.
이제 남은 것은 운영자가 화면 한 곳에서 아래를 같이 보는 구조다.

- 내부 `TransportPayment`
- `PaymentGatewayTransaction`
- Toss 실조회 결과
- cancel 이력
- billing agreement 상태
- auto-charge attempt
- payout item 상태
- 최신 payout/seller webhook 반영 결과

완료 기준:

- DB를 직접 보지 않아도 결제/지급 장애 지점을 파악할 수 있다.

---

## 3. 정책 보강 과제

### 3.1 결제 취소 정책

현재 코드는 아래 정책으로 좁혀져 있다.

- confirmed Toss transaction만 취소
- 내부 결제가 `PAID`일 때만 취소
- payout item이 생긴 뒤에는 취소 금지
- 부분취소는 미지원

남은 결정:

1. 부분취소를 열지 여부
2. 차주 확정 이후 취소 허용 여부
3. 주문 상태와 정산 상태를 어디까지 되돌릴지
4. 운영 감사 로그를 어디까지 남길지

### 3.2 billing/auto-charge 운영 정책

현재는 실제 charge client와 원장이 있다.
다만 운영 정책은 아직 얇다.

남은 결정:

1. billing 실패 재시도 횟수
2. overdue 전환 기준
3. billing key 저장/접근 통제 방식
4. 화주 재등록 유도 UX

### 3.3 payout 운영 정책

현재 payout 요청, polling, webhook 반영은 있다.
남은 것은 운영 제어와 예외 분류다.

남은 결정:

1. seller 상태 이상 시 운영자 개입 방식
2. payout 실패 코드 분류 기준
3. webhook와 polling 충돌 시 우선 규칙

---

## 4. 권장 실행 순서

1. 런타임 검증
- 현재 코드가 실제 Toss 응답과 맞는지 먼저 닫는다.

2. 운영 조회와 관리자 화면 연결
- 이미 있는 backend API를 운영에서 실제로 쓰게 만든다.

3. 취소 정책 확정
- 부분취소와 cancel 이후 상태 전이를 문서와 코드에서 고정한다.

4. billing/auto-charge UX 보강
- 화주와 관리자 화면 반영 범위를 닫는다.

5. payout 운영 UX 보강
- seller/payout 상태를 화면에서 추적 가능하게 만든다.

---

## 5. 이번 계획에서 제외하는 것

현재 코드와 문서 범위에서 당장 우선순위가 아닌 항목은 아래다.

- 결제위젯 신규 도입
- 브랜드페이 도입
- 일반 사용자용 환불 셀프서비스
- direct payment 방식 신규 도입

---

## 6. 완료 정의

아래가 모두 닫히면 현재 보강 라운드는 끝난 것으로 본다.

1. lookup, cancel, billing, payout webhook가 실환경 또는 테스트환경 기준으로 검증된다.
2. 관리자 화면에서 결제, billing, payout 상태를 한 번에 확인할 수 있다.
3. 부분취소와 after-payout cancel 정책이 문서와 코드에서 일치한다.
4. 화주 billing 등록/조회/해지와 수수료 자동청구 흐름이 운영 관점에서 닫힌다.

---

## 7. 연관 문서

- 상태 요약: `docs/toss-payment-project-summary.md`
- 상세 로직: `docs/payment-logic.md`
- 구현 스냅샷: `docs/toss-payment-implementation-handoff.md`
- 정합성 리뷰: `docs/toss-payment-gap-review.md`
- 테스트 앱 계획: `docs/toss-payment-test-app-plan.md`
