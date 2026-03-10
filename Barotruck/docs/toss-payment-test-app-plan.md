# 결제부터 차주 정산까지 테스트 앱 구축 계획

기준일: 2026-03-10

이 문서는 화주 결제 시작부터 차주 지급 상태 확인까지 한 번에 검증할 수 있는 전용 테스트 앱을 만들기 위한 계획서다.

상위 기준 문서:

1. `docs/payment-logic.md`
2. `docs/toss-payment-enhancement-plan.md`
3. `docs/toss-payment-gap-review.md`

---

## 1. 목표

테스트 앱에서 아래를 한 흐름으로 확인할 수 있어야 한다.

1. 화주 로그인
2. 결제 대상 주문 준비
3. 결제 수단 등록 또는 결제 준비
4. Toss 결제 실행
5. 내부 결제 상태 반영 확인
6. 차주 정산 대상 전환 확인
7. 관리자 지급 요청
8. payout / webhook / polling 상태 확인
9. 차주가 본인 지급 상태 확인

즉, 단순 API 점검 앱이 아니라 `결제 -> 내부 상태 반영 -> 정산 -> 지급 요청 -> 차주 수령 확인`까지 이어지는 E2E 테스트 앱이 목표다.

---

## 2. 테스트 앱에서 반드시 필요한 단계

### 2.1 사전 준비 단계

테스트 앱 첫 화면에서 아래 준비 상태를 보여줘야 한다.

1. 현재 서버 주소
2. 로그인 가능한 테스트 계정
3. Toss 결제 키 준비 상태
4. Toss payout 키 준비 상태
5. 현재 주문/정산 테스트 가능 여부

이 단계가 없으면 실제 결제 실패와 환경 미설정을 구분하기 어렵다.

### 2.2 시나리오 준비 단계

테스트 앱은 아래 두 방식을 지원하는 게 맞다.

1. 기존 주문 선택
2. 테스트용 주문 시나리오 생성

최소 시나리오 조건:

1. 운송 완료 주문
2. 결제 가능 상태
3. 차주 정보와 계좌 정보 존재
4. settlement 생성 가능 상태

가능하면 테스트 앱은 주문 ID를 직접 입력받는 수동 모드와, `테스트용 주문 1건 자동 준비` 모드를 같이 가져가는 게 좋다.

### 2.3 화주 결제 준비 단계

결제 전에 아래를 확인해야 한다.

1. 화주 billing agreement 존재 여부
2. 결제 방식 선택
3. 결제 예정 금액과 수수료
4. 현재 `TransportPayment` 상태
5. 현재 `Settlement` 상태

여기서 필요한 액션:

1. billing context 조회
2. billing agreement 조회
3. Toss prepare 호출
4. 수동 결제용 `mark-paid` 호출

### 2.4 화주 결제 실행 단계

테스트 앱에서 결제는 두 경로를 모두 지원하는 게 좋다.

1. Toss 결제창 결제
2. 수동 결제 반영

검증 포인트:

1. `TransportPayment = PAID`
2. `PaymentGatewayTransaction = PREPARED/CONFIRMED`
3. `Settlement = READY`
4. PG 응답 원문 확인

### 2.5 차주 확인 / 정산 전환 단계

차주가 결제를 확인하는 단계가 필요하다.

필요 기능:

1. 차주 로그인 전환
2. `driver confirm` 호출
3. 현재 정산 상태 조회

검증 포인트:

1. `TransportPayment = CONFIRMED`
2. `Order = CONFIRMED`
3. `Settlement = COMPLETED` 또는 차주 지급 대상 상태

### 2.6 관리자 지급 요청 단계

테스트 앱은 운영 관리자 웹을 열지 않고도 지급 요청을 보낼 수 있어야 한다.

필요 기능:

1. 관리자 로그인 전환
2. `POST /api/admin/payment/orders/{orderId}/payouts/request`
3. `POST /api/admin/payment/payout-items/orders/{orderId}/sync`
4. 배치 상태 조회

검증 포인트:

1. `DriverPayoutItem` 생성 여부
2. `status = REQUESTED / COMPLETED / FAILED`
3. `failureReason`
4. `sellerStatus`
5. `payoutRef`

### 2.7 payout / webhook 상태 추적 단계

실제 지급은 요청만으로 끝나지 않으므로 추적 단계가 따로 필요하다.

필요 기능:

1. polling 조회
2. 최신 webhook 수신 결과 확인
3. batch 상태 확인
4. payout item 상세 확인

검증 포인트:

1. `payout.changed`
2. `seller.changed`
3. `lastWebhookReceivedAt`
4. `lastWebhookProcessResult`
5. 내부 정산 상태와 payout 상태 일치 여부

### 2.8 차주 수령 확인 단계

최종적으로 차주가 테스트 앱에서 자기 지급 상태를 확인할 수 있어야 한다.

필요 기능:

1. 차주 토큰으로 `/api/v1/payments/payouts/orders/{orderId}/status` 조회
2. 주문별 지급 상태 타임라인 표시
3. 최종 수령 확인 체크

최종 완료 기준:

1. 차주 화면에서 `COMPLETED`
2. 배치 상태도 완료
3. 내부 정산과 payout item 상태 일치

### 2.9 결과 저장 단계

테스트 앱은 마지막에 아래를 기록해야 한다.

1. 주문 ID
2. paymentKey / pgOrderId / payoutRef
3. 각 단계별 상태
4. 실패 이유
5. raw payload

이 단계가 있어야 QA와 회귀 검증이 가능하다.

---

## 3. 테스트 앱 화면 구성 권장안

### 3.1 메인 구조

1. 환경 점검
2. 계정 전환
3. 시나리오 준비
4. 화주 결제
5. 차주 확인
6. 관리자 지급
7. 차주 수령 확인
8. raw payload / 타임라인

### 3.2 권장 파일 구조

RN 앱 기준 권장 경로:

- `FrontNew/barotruck-app/app/(common)/tools/payment-e2e-lab.tsx`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/sections/*`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/model/*`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/lib/*`
- `FrontNew/barotruck-app/src/shared/api/paymentTestService.ts`

### 3.3 필수 공용 상태

테스트 앱에서 한 번에 관리해야 하는 상태:

1. 현재 서버 URL
2. 현재 actor
3. 선택된 orderId
4. payment snapshot
5. settlement snapshot
6. payout snapshot
7. latest webhook snapshot
8. activity log

---

## 4. 백엔드에서 추가되면 좋은 보조 API

현재도 검증은 가능하지만, 테스트 앱 생산성을 위해 아래 보조 API가 있으면 좋다.

### 4.1 시나리오 준비 API

예시:

- `POST /api/test/payment-scenarios/orders`
- `POST /api/test/payment-scenarios/orders/{orderId}/complete-transport`

역할:

- 테스트용 주문 생성
- 운송 완료 상태까지 빠르게 이동

### 4.2 통합 스냅샷 API

예시:

- `GET /api/test/payment-scenarios/orders/{orderId}/snapshot`

포함 정보:

1. order
2. transport payment
3. settlement
4. gateway transaction
5. payout item
6. webhook events

### 4.3 상태 초기화 API

예시:

- `POST /api/test/payment-scenarios/orders/{orderId}/reset`

역할:

- 같은 주문으로 반복 테스트할 수 있게 초기화

주의:

- 운영에는 절대 열지 않고 local/dev 전용으로 제한

---

## 5. 구현 순서

1. 테스트 앱 메인 쉘과 환경 점검 화면
2. 계정 전환과 주문 선택
3. 화주 결제 준비/실행 섹션
4. 차주 확인 섹션
5. 관리자 지급 요청/동기화 섹션
6. 차주 수령 확인 섹션
7. raw payload / activity log 섹션
8. 필요 시 백엔드 test-support API 추가

---

## 6. 분업안

### 에이전트 1. 테스트 앱 셸 / 공통 상태

범위:

- 라우트 생성
- 공통 스토어/상태
- 환경 점검 섹션
- 계정 전환 섹션

완료 기준:

- 한 화면에서 actor, orderId, 서버 상태를 공통으로 관리

### 에이전트 2. 화주 결제 단계

범위:

- billing agreement 조회
- Toss prepare / 결제 진입
- 수동 결제 경로
- 결제 후 snapshot 갱신

완료 기준:

- 화주가 테스트 앱 안에서 결제 성공/실패까지 확인 가능

### 에이전트 3. 차주 확인 및 수령 상태

범위:

- driver confirm
- 차주 정산 상태 조회
- 차주 payout status 조회

완료 기준:

- 차주 기준으로 결제 확인과 지급 수령 상태를 모두 볼 수 있음

### 에이전트 4. 관리자 지급 운영

범위:

- payout request
- payout sync
- batch status
- payout item detail

완료 기준:

- 테스트 앱 안에서 관리자 지급 운영까지 진행 가능

### 에이전트 5. 백엔드 테스트 보조 API / 스냅샷

범위:

- 필요 시 test-support API
- 통합 snapshot 응답
- 주문 준비/초기화 보조 기능

완료 기준:

- 수동 DB 조작 없이 시나리오 준비와 상태 확인이 가능

### 에이전트 6. QA / 결과 기록

범위:

- 단계별 성공/실패 체크리스트
- raw payload 저장 형식
- 테스트 결과 요약 문서

완료 기준:

- 한 번 테스트 후 재현 가능한 결과 문서가 남음

---

## 7. 에이전트별 복붙용 프롬프트

### 7.1 에이전트 1 프롬프트

```text
너는 RN 테스트 앱의 셸/공통상태 담당이다.

작업 루트:
C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app

먼저 읽기:
- C:\ytheory\springPro\Backend\Barotruck\docs\toss-payment-test-app-plan.md
- C:\ytheory\springPro\Backend\Barotruck\docs\payment-logic.md

목표:
- 결제부터 차주 정산까지 테스트하는 전용 앱 화면의 뼈대를 만든다.

네 범위:
1. 새 라우트 생성
2. 공통 상태 모델 정의
3. 환경 점검 섹션
4. 계정 전환 섹션
5. orderId 공통 선택 상태

권장 경로:
- app/(common)/tools/payment-e2e-lab.tsx
- src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx
- src/features/common/tools/payment-e2e-lab/model/*
- src/features/common/tools/payment-e2e-lab/lib/*

제한:
- 화주 결제 로직, 차주 로직, 관리자 지급 로직은 직접 구현하지 말고 자리만 만든다.
- 기존 운영 화면은 건드리지 마라.

완료 기준:
- 한 화면에서 actor, orderId, 서버 상태를 공통으로 관리할 수 있다.
- 후속 에이전트가 섹션을 붙일 수 있는 구조여야 한다.

검증:
- npx tsc --noEmit
```

### 7.2 에이전트 2 프롬프트

```text
너는 RN 테스트 앱의 화주 결제 단계 담당이다.

작업 루트:
C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app

먼저 읽기:
- C:\ytheory\springPro\Backend\Barotruck\docs\toss-payment-test-app-plan.md
- C:\ytheory\springPro\Backend\Barotruck\docs\payment-logic.md

목표:
- 테스트 앱에서 화주가 billing 상태 확인, 결제 준비, Toss 결제 실행, 수동 결제 반영까지 할 수 있게 만든다.

네 범위:
1. billing context 조회
2. billing agreement 조회
3. Toss prepare 호출
4. payment checkout 진입
5. mark-paid 수동 결제 경로
6. 결제 후 snapshot 갱신

권장 위치:
- src/features/common/tools/payment-e2e-lab/ui/sections/shipper-payment-section.tsx
- src/shared/api/paymentTestService.ts

제한:
- 관리자 지급 기능은 건드리지 마라.
- 차주 수령 확인은 직접 구현하지 마라.

완료 기준:
- 화주 단계만으로 결제 성공/실패와 내부 상태 변화를 확인할 수 있다.

검증:
- npx tsc --noEmit
```

### 7.3 에이전트 3 프롬프트

```text
너는 RN 테스트 앱의 차주 단계 담당이다.

작업 루트:
C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app

먼저 읽기:
- C:\ytheory\springPro\Backend\Barotruck\docs\toss-payment-test-app-plan.md
- C:\ytheory\springPro\Backend\Barotruck\docs\payment-logic.md

목표:
- 테스트 앱에서 차주가 결제 확인과 지급 상태 수령 확인을 할 수 있게 만든다.

네 범위:
1. driver confirm 호출
2. 차주 정산 상태 조회
3. 차주 payout status 조회
4. 상태 타임라인 표시

권장 위치:
- src/features/common/tools/payment-e2e-lab/ui/sections/driver-settlement-section.tsx
- src/shared/api/paymentTestService.ts

제한:
- 관리자 지급 요청은 직접 구현하지 마라.

완료 기준:
- 차주 단계에서 결제 확인 이후 상태와 최종 지급 상태를 확인할 수 있다.

검증:
- npx tsc --noEmit
```

### 7.4 에이전트 4 프롬프트

```text
너는 RN 테스트 앱의 관리자 지급 운영 단계 담당이다.

작업 루트:
C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app

먼저 읽기:
- C:\ytheory\springPro\Backend\Barotruck\docs\toss-payment-test-app-plan.md
- C:\ytheory\springPro\Backend\Barotruck\docs\payment-logic.md

목표:
- 테스트 앱 안에서 관리자 지급 요청과 상태 동기화를 할 수 있게 만든다.

네 범위:
1. payout request 호출
2. payout sync 호출
3. batch status 조회
4. payout item detail 조회
5. failureReason, sellerStatus, payoutRef 표시

권장 위치:
- src/features/common/tools/payment-e2e-lab/ui/sections/admin-payout-section.tsx
- src/shared/api/paymentTestService.ts

제한:
- 운영 관리자 웹은 건드리지 마라.

완료 기준:
- 테스트 앱에서 관리자 지급 요청과 결과 확인이 가능하다.

검증:
- npx tsc --noEmit
```

### 7.5 에이전트 5 프롬프트

```text
너는 백엔드 테스트 보조 API 담당이다.

작업 루트:
C:\ytheory\springPro\Backend\Barotruck

먼저 읽기:
- docs/toss-payment-test-app-plan.md
- docs/payment-logic.md
- docs/payment-postman-test-cases.md

목표:
- 테스트 앱 생산성을 위해 필요한 test-support API 또는 통합 snapshot API를 만든다.

네 범위:
1. order snapshot API
2. payment/settlement/payout/webhook 통합 조회 응답
3. 필요 시 local/dev 전용 시나리오 준비 API

예시:
- GET /api/test/payment-scenarios/orders/{orderId}/snapshot
- POST /api/test/payment-scenarios/orders
- POST /api/test/payment-scenarios/orders/{orderId}/reset

제한:
- 운영 노출 금지
- local/dev 전용으로 제한
- 기존 운영 API를 깨지 마라

완료 기준:
- 테스트 앱에서 DB 직접 조회 없이 상태 스냅샷을 받을 수 있다.

검증:
- ./gradlew.bat compileJava
```

### 7.6 에이전트 6 프롬프트

```text
너는 QA/결과 기록 담당이다.

작업 루트:
C:\ytheory\springPro\Backend\Barotruck

먼저 읽기:
- docs/toss-payment-test-app-plan.md
- docs/payment-postman-test-cases.md
- docs/toss-payment-gap-review.md

목표:
- 테스트 앱에서 수행한 결제 -> 정산 -> 지급 -> 차주 수령 확인 결과를 재현 가능한 문서로 남긴다.

네 범위:
1. 테스트 체크리스트 작성
2. 단계별 기대 결과 표 작성
3. raw payload 기록 형식 정리
4. 실패 시 진단 순서 정리

권장 산출물:
- docs/payment-test-app-qa-checklist.md
- docs/payment-test-app-runbook.md

완료 기준:
- QA 담당자가 앱만 보고 동일 시나리오를 재현할 수 있다.
```

---

## 8. 연관 문서

- 로직 기준: `docs/payment-logic.md`
- 남은 보강 계획: `docs/toss-payment-enhancement-plan.md`
- 미비점 리뷰: `docs/toss-payment-gap-review.md`
- 기존 분업 문서: `docs/toss-payment-team-split-plan.md`
- API 샘플: `docs/payment-postman-test-cases.md`
