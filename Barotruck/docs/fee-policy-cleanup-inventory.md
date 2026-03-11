# 양면 수수료 정책 정리 대상 목록

기준일: 2026-03-11

이 문서는 현재 구현 기준으로 유지해야 하는 것과 후속 정리만 남은 것을 다시 분류한 목록이다.

---

## 1. 유지해야 하는 핵심 문서

- [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)
- [fee-policy-contracts.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-contracts.md)
- [fee-policy-snapshot-fields.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-snapshot-fields.md)
- [fee-policy-agent-ownership.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-agent-ownership.md)
- [fee-policy-agent-prompts.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-agent-prompts.md)
- [promotion-eligibility-rules.md](c:/ytheory/springPro/Backend/Barotruck/docs/promotion-eligibility-rules.md)
- [fee-policy-admin-transition.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-admin-transition.md)
- [two-sided-fee-policy-cutover-runbook.md](c:/ytheory/springPro/Backend/Barotruck/docs/two-sided-fee-policy-cutover-runbook.md)
- [fee-policy-implementation-audit.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-implementation-audit.md)

현재 기준으로 위 문서들은 `검토 필요`가 아니라 유지 문서다.

---

## 2. 이미 정리된 항목

- preview DTO 이중화
  - 상태: 정리 완료
  - 조치: `FeePreviewResponse.java` 제거, `FeeBreakdownPreviewResponse`만 유지

- 앱 옛 preview 경로 사용
  - 상태: 정리 완료
  - 조치: 앱 preview를 `/api/v1/payments/fee-preview`로 이관

- Toss `3%` vs `10%` 문서 충돌
  - 상태: 정리 완료
  - 조치: 전환 문서 기본값을 `10.0%`로 수정

- 관리자 웹 report API 시그니처 불일치
  - 상태: 정리 완료
  - 조치: `updateReportStatus(reportId, status, days?)`로 정리

---

## 3. 후속 정리만 남은 항목

### 테스트/운영 보조 화면 노출 여부

아래 파일은 기능 구현 자체보다 테스트/운영 도구에 가깝다.

- `src/main/resources/static/toss-test.html`
- `src/main/resources/static/toss-live-test.html`
- `src/main/resources/static/payment-api-test.html`
- `src/main/resources/static/payment-api-test.js`
- `src/main/resources/static/payment-api-test-config.js`
- `src/main/resources/static/admin-payment-test.html`
- `src/main/resources/static/admin-payment-cycles.html`
- `src/main/resources/static/admin-payment-cycle.html`
- `src/main/resources/static/admin-payment-cycle.js`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx`

권장 조치:

- prod 메뉴/라우트 노출 여부를 별도 판단
- 가능하면 dev/test flag 뒤로 이동

### legacy alias deprecation 순서

대상:

- `TransportPaymentResponse.amount`
- `TransportPaymentResponse.feeRateSnapshot`
- `TransportPaymentResponse.feeAmountSnapshot`
- `TransportPaymentResponse.netAmountSnapshot`

현재는 하위 호환 때문에 유지하지만, 장기적으로는 `amountSnapshot` 중심 read로 옮기는 순서를 문서화해야 한다.

### snapshot fallback 설명 보강

대상:

- `PaymentAmountSnapshotResponse.java`

현재 fallback 숫자는 현행 정책과 맞지만, legacy 주문에서 어떤 경우에 fallback을 허용하는지 운영 문구를 보강하는 편이 안전하다.

---

## 4. 삭제하면 안 되는 파일

- `TransportPaymentPricingSnapshot.java`
- `PaymentAmountSnapshotResponse.java`
- bilateral snapshot이 확장된 `TransportPayment.java`
- bilateral snapshot이 확장된 `FeeInvoiceItem.java`
- bilateral snapshot이 확장된 `DriverPayoutItem.java`
- 관련 단위 테스트 전부

이 파일들은 현재 구조에서 중간 adapter 또는 canonical snapshot 역할을 하므로 제거 대상이 아니다.

---

## 5. 결론

현재 cleanup의 핵심은 `미구현 기능 보완`이 아니라 아래 두 가지다.

1. 테스트/운영 보조 도구의 노출 범위 정리
2. legacy alias -> bilateral canonical deprecation 순서 확정

preview 계약, 앱 preview 경로, Toss 기본값 충돌은 이미 정리된 상태다.
