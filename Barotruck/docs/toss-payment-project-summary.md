# Barotruck Toss 결제 프로젝트 요약

기준일: 2026-03-10

이 문서는 현재 백엔드 코드 기준으로 Toss 결제 관련 기능을 빠르게 판단하기 위한 요약본이다.

상태 표기:

- `구현됨`: 코드, API, 배치 연결이 완료된 상태
- `부분 구현`: 핵심 로직은 있으나 운영 조회, 정책, UI, 실검증 중 일부가 남은 상태
- `운영 미검증`: 코드상 존재하지만 실키 또는 실웹훅 기준 검증 결과가 문서화되지 않은 상태
- `없음`: 현재 코드 범위에서 관련 구현이 없는 상태

판단 범위:

- 이 문서는 `C:\ytheory\springPro\Backend\Barotruck` 백엔드 코드 기준이다.
- 관리자 웹과 앱의 실제 화면 반영 여부는 이 저장소만으로 확정하지 않는다.

문서 우선순위:

1. `docs/payment-logic.md`
2. `docs/toss-payment-project-summary.md`
3. `docs/toss-payment-enhancement-plan.md`
4. `docs/toss-payment-implementation-handoff.md`
5. `docs/toss-payment-gap-review.md`

---

## 1. 결론

현재 상태를 한 줄로 정리하면 아래와 같다.

1. 화주 단건 결제, 결제 웹훅, 관리자 lookup/cancel, billing agreement, fee auto charge, payout webhook까지 백엔드 코드는 이미 들어가 있다.
2. 예전 문서의 `없음` 표현 중 상당수는 더 이상 맞지 않고 `부분 구현` 또는 `구현됨`으로 올려야 한다.
3. 남은 핵심 공백은 `실운영 런타임 검증`, `부분취소 등 세부 정책`, `관리자 웹/앱 연동 확인`이다.

---

## 2. 기능별 현재 상태

| 영역 | 현재 상태 | 코드 기준 판단 | 남은 미비점 |
|---|---|---|---|
| 화주 단건 결제창 결제 | 구현됨 | `prepare -> Toss confirm -> 내부 결제 반영` 흐름과 사용자 API가 존재한다. | 실키 기준 confirm 런타임 검증 결과가 문서화되지 않았다. |
| 결제 승인 후 웹훅 후속 반영 | 구현됨 | `POST /api/v1/payments/webhooks/toss`, 멱등 처리, confirm/webhook 순서 역전 보정이 구현돼 있다. | 실 payload 기준 재현 테스트 기록이 없다. |
| 운영 복구성 처리 | 부분 구현 | expire prepared, retry queue, reconciliation, 관리자용 Toss lookup/cancel API, 상태 조회 API가 있다. | 통합 운영 화면과 단일 대시보드 수준의 조회는 아직 약하다. |
| 차주 지급대행(Payout) | 부분 구현 | seller 등록, payout 요청, polling 동기화, `payout.changed`/`seller.changed` 웹훅 반영이 구현돼 있다. | 실지급 및 실웹훅 검증 결과가 없고, 관리자 UI 연결 여부가 문서에 없다. |
| 자동결제(빌링키) | 부분 구현 | billing context, billing key 발급/조회/해지 API, 실제 Toss auto-charge client, 시도 원장이 존재한다. | 화주 UI, 관리자 UI, 실 billing issue/charge/delete 검증이 남아 있다. |
| Toss 결제 취소/환불 | 부분 구현 | 관리자 `cancel` API와 Toss cancel 클라이언트, 내부 결제 취소 반영 로직이 있다. | 부분취소 미지원, 셀프서비스 없음, 실 cancel 검증이 남아 있다. |
| 결제위젯 | 없음 | 위젯 전용 서버 계약이나 사용 코드가 없다. | 필요 시 신규 도입 범위다. |
| API 직접 연동(hosted/direct) | 없음 | `POST /v1/payments` 중심의 직접 생성 흐름이 없다. | 필요 시 신규 도입 범위다. |
| 브랜드페이 | 없음 | 관련 도메인, API, UI 흔적이 없다. | 필요 시 신규 도입 범위다. |

---

## 3. 지금 가장 중요한 남은 작업

### 3.1 런타임 검증

코드상 존재하지만 아직 문서로 닫히지 않은 항목은 아래다.

- Toss payment lookup 실제 호출
- Toss payment cancel 실제 호출
- billing key issue/charge/delete 실제 호출
- `payout.changed`, `seller.changed` 샘플 payload 반영

즉, `구현됨`과 `운영 완료`는 아직 같은 의미가 아니다.

### 3.2 운영 관측성 정리

관리자 조회 API는 이미 많이 늘어났지만, 운영자가 한 화면에서 아래를 한 번에 보는 구조는 문서상 아직 닫히지 않았다.

- 내부 결제 상태
- Toss 실조회 결과
- cancel 이력
- billing agreement 상태와 auto-charge attempt
- payout item 상태와 최신 webhook 반영 결과

### 3.3 정책 미세 조정

현 시점에서 정책으로 남아 있는 대표 항목은 아래다.

- 부분취소 허용 여부
- payout item 생성 이후 cancel 허용 여부
- billing 실패 재시도 및 overdue 운영 정책
- billing key 저장/접근 정책

---

## 4. 추천 읽기 순서

1. `docs/payment-logic.md`
- 현재 코드 동작과 상태 전이를 먼저 본다.

2. `docs/toss-payment-project-summary.md`
- 어떤 항목이 `구현됨`, `부분 구현`, `운영 미검증`, `없음`인지 빠르게 판단한다.

3. `docs/toss-payment-enhancement-plan.md`
- 아직 남은 작업만 추려서 본다.

4. `docs/toss-payment-implementation-handoff.md`
- 최근 백엔드 확장 범위와 재개 지점을 본다.

5. `docs/toss-payment-gap-review.md`
- 문서 정합성 관점에서 남은 리스크를 점검한다.

---

## 5. 연관 문서

- 상세 로직: `docs/payment-logic.md`
- 남은 작업 계획: `docs/toss-payment-enhancement-plan.md`
- 구현 스냅샷: `docs/toss-payment-implementation-handoff.md`
- 정합성 리뷰: `docs/toss-payment-gap-review.md`
