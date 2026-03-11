# 관리자 정산/결제 보조 API 활용 계획

## 0. 문서 목적
이 문서는 관리자 웹에서 이미 존재하지만 메인 플로우에 아직 충분히 활용하지 않는 정산/결제 API를 어떻게 사용할지 정리한 실행 계획서다.

기준:
- 수수료 정책 API(`fee-policy*`)는 이 문서 범위에서 제외한다.
- 메인 상태 변경은 이미 구현된 단일 상태 변경 API를 기준으로 유지한다.
  - 결제: `PATCH /api/admin/payment/orders/{orderId}/status`
  - 정산: `PATCH /api/v1/settlements/orders/{orderId}/status`

관련 소스:
- 백엔드
  - `src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java`
  - `src/main/java/com/example/project/domain/payment/controller/PaymentController.java`
  - `src/main/java/com/example/project/domain/settlement/controller/SettlementController.java`
- 프론트
  - `Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts`
  - `Admin_FrontEnd/barotruck_admin_web/app/page.tsx`
  - `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx`
  - `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/page.tsx`
  - `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`

---

## 1. 범위

### 1.1 메인 플로우로 유지할 API
이 두 개는 이미 메인 운영 API다.

- `PATCH /api/admin/payment/orders/{orderId}/status`
- `PATCH /api/v1/settlements/orders/{orderId}/status`

### 1.2 활용 계획 대상 API
이 문서에서 구체적으로 계획하는 API는 아래 5개다.

- `GET /api/v1/settlements/admin/status-summary`
- `POST /api/v1/payments/orders/{orderId}/mark-paid`
- `POST /api/admin/payment/orders/{orderId}/disputes`
- `PATCH /api/admin/payment/orders/{orderId}/disputes/{disputeId}/status`

### 1.3 제외
- `GET/PATCH/POST /api/admin/payment/fee-policy*`
- 이미 운영 중인 메인 상태 변경 화면

---

## 2. 현재 화면 구조 기준 붙일 위치

현재 관리자 웹에서 보조 API를 붙일 수 있는 실제 위치는 아래와 같다.

1. 대시보드 메인
- 경로: `/`
- 파일: `Admin_FrontEnd/barotruck_admin_web/app/page.tsx`

2. 차주 정산 목록
- 경로: `/global/billing/settlement/driver`
- 파일: `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx`

3. 화주 정산 목록
- 경로: `/global/billing/settlement/shipper`
- 파일: `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/page.tsx`

4. 화주 정산 상세
- 경로: `/global/billing/settlement/shipper/[id]`
- 파일: `Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`

---

## 3. API별 구체적 사용 계획

## 3.1 `GET /api/v1/settlements/admin/status-summary`

### 역할
- 전사 운영 현황을 빠르게 보는 관리자 대시보드 요약 API
- 전체 목록을 다 가져오기 전에 상단 카드 숫자를 먼저 보여주는 용도

### 붙일 위치
- 1차 적용: 대시보드 메인 `/`
- 붙일 섹션: 현재 상단 3카드 아래 또는 우측에 `정산 운영 요약` 카드 그룹 추가

### 화면 구성
- 총 정산 금액
- 정산 대기 금액
- 정산 완료 금액
- 총 건수 / 대기 건수 / 완료 건수

### 구현 방식
- `app/page.tsx`에서 현재 `paymentAdminApi.getSettlements("COMPLETED")`와 별도로 호출
- 기존 “최근 정산 완료 내역” 리스트는 목록 API 유지
- 상단 카드 숫자만 summary API로 분리

### 새로 만들 것
- 제안 컴포넌트: `Admin_FrontEnd/barotruck_admin_web/app/features/dashboard/settlement_summary_card.tsx`

### 주의
- 화주 결제 상태 전체를 보여주는 API가 아니라 정산 상태 중심 요약이다.
- 화주 정산 화면의 전광판 기준 데이터로 교체하지 않는다.

---

## 3.2 `POST /api/v1/payments/orders/{orderId}/mark-paid`

### 역할
- 화주 결제 상태를 한 번에 `PAID`로 올리는 빠른 액션
- 상태 셀렉터보다 클릭 수가 적은 운영용 버튼

### 붙일 위치
- 1차 적용: 화주 정산 목록 `/global/billing/settlement/shipper`
- 2차 적용: 화주 정산 상세 `/global/billing/settlement/shipper/[id]`

### 화면 구성
- 목록 행 우측에 `입금 반영` 버튼 추가
- 상세 화면 상단 액션 영역에 `입금 반영` 버튼 추가

### 버튼 노출 조건
- 현재 결제 상태가 `READY`인 경우만 노출
- 이미 `PAID`, `CONFIRMED`, `ADMIN_FORCE_CONFIRMED`면 숨김 또는 비활성화

### 구현 방식
- 메인 상태 셀렉터는 유지
- 빠른 버튼은 `markPaymentPaid`를 직접 호출
- 성공 시 목록/상세 데이터를 다시 조회

### 새로 만들 것
- 제안 컴포넌트: `Admin_FrontEnd/barotruck_admin_web/app/features/shared/components/payment_quick_actions.tsx`

### 기대 효과
- 운영자가 가장 자주 쓰는 `READY -> PAID`를 별도 버튼으로 분리
- 메인 셀렉터를 열지 않아도 즉시 처리 가능

---

## 3.3 `POST /api/admin/payment/orders/{orderId}/disputes`

### 역할
- 주문 단위 분쟁 생성
- 메인 상태 셀렉터가 아니라 “분쟁 등록”이라는 명시적 액션으로 사용

### 붙일 위치
- 1차 적용: 화주 정산 상세 `/global/billing/settlement/shipper/[id]`
- 2차 적용: 화주 정산 목록에서 `분쟁 처리` 버튼 클릭 시 모달 오픈

### 화면 구성
- 분쟁 생성 모달
- 입력값:
  - `reasonCode`
  - `description`
  - `attachmentUrl` 또는 증빙 입력

### 구현 방식
- 목록에서는 버튼만 두고 실제 입력은 모달에서 수행
- 상세 화면에서는 타임라인 카드 아래에 `분쟁 생성` 버튼 배치

### 새로 만들 것
- 제안 컴포넌트: `Admin_FrontEnd/barotruck_admin_web/app/features/shared/components/payment_dispute_create_modal.tsx`

### 주의
- 이 API는 일반 상태 변경용 대체 수단으로 쓰지 않는다.
- “분쟁을 생성한다”는 의도가 명확할 때만 호출한다.

---

## 3.4 `PATCH /api/admin/payment/orders/{orderId}/disputes/{disputeId}/status`

### 역할
- 이미 생성된 분쟁의 관리자 처리
- `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED` 전용

### 붙일 위치
- 1차 적용: 화주 정산 상세 `/global/billing/settlement/shipper/[id]`
- 상세 화면 안의 `분쟁 처리 패널`에서 사용

### 화면 구성
- 현재 분쟁 상태 배지
- 관리자 메모 입력창
- 액션 버튼
  - `보류`
  - `강제 확정`
  - `반려`

### 구현 방식
- 상세 페이지 진입 시 분쟁 조회 API가 필요하면 추가 조회 또는 결제 상태 기준 보조 표시
- 모달보다 상세 패널이 더 맞다. 분쟁은 이력과 메모를 같이 봐야 하기 때문이다.

### 새로 만들 것
- 제안 컴포넌트: `Admin_FrontEnd/barotruck_admin_web/app/features/shared/components/payment_dispute_resolution_panel.tsx`

### 주의
- 화주 정산 목록 테이블에 버튼을 다 넣으면 행 밀도가 너무 높아진다.
- 목록에서는 `분쟁 처리` 진입 버튼만 두고, 실제 변경은 상세 화면에서 한다.

---

## 4. 새로 만들 화면/컴포넌트 계획

## 4.1 바로 만들 것
1. 대시보드 정산 요약 카드
- 파일 후보: `app/features/dashboard/settlement_summary_card.tsx`

2. 화주 정산 빠른 액션 버튼 묶음
- 파일 후보: `app/features/shared/components/payment_quick_actions.tsx`

3. 분쟁 생성 모달
- 파일 후보: `app/features/shared/components/payment_dispute_create_modal.tsx`

4. 분쟁 처리 패널
- 파일 후보: `app/features/shared/components/payment_dispute_resolution_panel.tsx`

## 4.2 먼저 개편할 화면
가장 먼저 손볼 화면은 `화주 정산 상세`다.

이유:
- 지금 상세 페이지는 `fetchOrders()` 기반 단순 주문 표시라 정산/결제/분쟁 운영 화면 역할을 못 한다.
- 보조 API를 가장 자연스럽게 붙일 수 있는 위치가 상세 화면이다.
- 목록에 액션을 과도하게 넣지 않고 상세로 역할을 분리할 수 있다.

상세 페이지 개편 목표:
- 주문 기본 정보
- 결제 상태/결제 메타 정보
- 정산 상태
- 빠른 액션 버튼
- 분쟁 생성/처리 패널

---

## 5. 구현 우선순위

1. 대시보드에 `status-summary` 연결
- 난이도 낮음
- 사용자 가치 즉시 확인 가능

2. 화주 정산 목록에 `입금 반영` 빠른 버튼 추가
- 가장 자주 쓰는 운영 액션

3. 화주 정산 상세 페이지 개편
- 주문 상세 중심 화면에서 결제/분쟁 운영 화면으로 전환

4. 상세 화면에 분쟁 생성/처리 UI 추가
- `createDispute`, `updateDisputeStatus` 연결

## 6. 채택/비채택 정리

### 채택
- `GET /api/v1/settlements/admin/status-summary`
- `POST /api/v1/payments/orders/{orderId}/mark-paid`
- `POST /api/admin/payment/orders/{orderId}/disputes`
- `PATCH /api/admin/payment/orders/{orderId}/disputes/{disputeId}/status`

### 제거 완료
- `PATCH /api/v1/settlements/{orderId}/complete`
- `PATCH /api/v1/settlements/orders/{orderId}/complete-by-user`
- `POST /api/v1/settlements/init`

제거 이유:
- 신규 관리자 UI 기준으로 `updateSettlementStatus`와 역할이 중복되거나 불필요했다.
- 정산 상태 변경 기준을 단일 API로 고정하기 위해 제거했다.

---

## 7. 완료 기준

1. 화주 정산 목록에서 `입금 반영` 빠른 액션이 동작한다.
2. 화주 정산 상세에서 분쟁 생성과 분쟁 처리까지 끝난다.
3. 대시보드 상단에서 정산 요약 수치가 분리되어 보인다.
4. 관리자 운영자는 메인 상태 변경과 분쟁 처리의 역할 차이를 화면에서 직관적으로 구분할 수 있다.
