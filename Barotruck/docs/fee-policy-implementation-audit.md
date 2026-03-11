# 양면 수수료 정책 구현 감사

기준일: 2026-03-11

이 문서는 현재 양면 수수료 정책 구현 상태를 코드 기준으로 다시 점검한 결과다.

검증 범위:

- 백엔드: `C:\ytheory\springPro\Backend\Barotruck`
- 앱: `C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app`
- 관리자 웹: `C:\ytheory\Backend\Barotruck\Admin_FrontEnd\barotruck_admin_web`

---

## 1. 검증 결과 요약

### 통과

- 백엔드 `.\gradlew.bat compileJava`
- 백엔드 preview/policy 핵심 테스트
  - `PaymentControllerFeePreviewTest`
  - `PaymentFeePreviewServiceTest`
  - `FeePolicyServiceTest`
  - `MarketplaceFeeCalculationServiceTest`
- 앱 `npx tsc --noEmit`
- 관리자 웹 `npx tsc --noEmit`

### 현재 결론

- preview 계약은 flat bilateral DTO 하나로 통일됐다.
- 앱은 새 canonical preview endpoint를 사용한다.
- Toss 기본 정책값은 `10%`로 맞춰졌다.
- 관리자 프런트 타입 에러는 정리됐다.

---

## 2. 실제 반영된 것

### preview 계약 통일

- `PaymentController.java`
- `TransportPaymentService.java`
- `PaymentFeePreviewService.java`
- `FeeBreakdownPreviewResponse.java`

사용자 preview와 관리자 preview는 모두 `FeeBreakdownPreviewResponse`를 중심으로 움직인다.

정리된 점:

- `POST /api/v1/payments/fee-preview`가 flat bilateral breakdown을 반환한다.
- `previewMode`, `paymentProvider`, `shipperPromoEligible`, `driverPromoEligible`, `negativeMargin`, `policyConfigId`가 함께 내려간다.
- 미배정 draft preview에서는 driver-side 필드를 `null`로 내려준다.

### 앱 preview 이관

- `FrontNew/barotruck-app/src/shared/api/paymentService.ts`
- `FrontNew/barotruck-app/src/shared/models/feePolicy.ts`
- `FrontNew/barotruck-app/src/shared/utils/feePolicy.ts`

정리된 점:

- 앱은 더 이상 `/api/v1/orders/fare-preview`를 사용하지 않는다.
- `POST /api/v1/payments/fee-preview`를 사용한다.
- 앱 fee 모델은 bilateral preview 응답을 읽고, 화면에는 shipper snapshot으로 정규화해서 사용한다.
- fallback 기본값도 `side 2.5/2.0/1.8/1.5`, promo `1.5%`, Toss `10%` 기준으로 맞췄다.

### Toss 정책 정합화

- `FeePolicyService.java`
- `MarketplaceFeeMath.java`
- `fee-policy-admin-transition.md`

정리된 점:

- 기본 Toss rate는 `0.1000`
- Toss 수수료는 `baseAmount` 기준 선차감 후, 남은 금액에서 side fee를 계산
- 관리자 전환 문서의 예시도 `10.0%`로 수정

### 관리자 프런트 정리

- `app/features/shared/api/payment_admin_api.ts`
- `app/features/shared/api/report_api.ts`

정리된 점:

- `TransportPaymentResponse.amountSnapshot` 타입이 반영됐다.
- `reportApi.updateReportStatus(reportId, status, days?)` 시그니처가 현재 화면 호출과 맞는다.

---

## 3. 남은 기술 부채

### 선택적 정리 대상

- 테스트/운영용 static payment pages 노출 여부
- legacy alias 필드와 bilateral canonical 필드의 장기 deprecation 순서
- `PaymentAmountSnapshotResponse`의 legacy fallback 규칙 명문화

현재 이 항목들은 구현 blocker가 아니라 후속 정리 대상이다.

---

## 4. 삭제 또는 정리된 것

- nested preview 전용 DTO `FeePreviewResponse.java`는 더 이상 사용되지 않아 제거했다.
- `preview 계약 2개 공존` 문제는 닫혔다.
- `앱이 옛 preview API를 호출` 문제는 닫혔다.
- `관리자 웹 Expected 2 arguments, but got 3` 문제는 닫혔다.

---

## 5. 결론

2026-03-11 기준, 양면 수수료 정책의 핵심 구현은 아래 조건을 만족한다.

1. 사용자 preview와 관리자 preview가 같은 bilateral 계약을 쓴다.
2. 앱 오더 생성 preview가 실제 백엔드 계산 결과를 읽는다.
3. 기본 Toss 정책과 문서 기준이 `10%`로 일치한다.
4. 백엔드, 앱, 관리자 웹 검증이 모두 green 상태다.

남은 일은 기능 미구현보다 `운영 노출 정리`와 `legacy deprecation 순서 문서화`에 가깝다.
