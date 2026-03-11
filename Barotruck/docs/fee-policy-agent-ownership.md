# 양면 수수료 정책 에이전트 파일 소유권

기준일: 2026-03-11

이 문서는 [fee-policy-agent-prompts.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-agent-prompts.md)의 에이전트들이 파일 충돌 없이 병렬 작업할 수 있도록 소유 범위를 고정한다.

원칙:

- 한 파일에는 `primary owner`를 1명만 둔다.
- 다른 에이전트가 해당 파일을 수정해야 하면 owner와 먼저 합의한다.
- contract 문서가 코드보다 우선한다.

---

## 1. 에이전트별 1차 소유권

| 에이전트 | 1차 소유 범위 | 핵심 파일 |
| --- | --- | --- |
| `0` | 계약 문서 | `docs/fee-policy-contracts.md`, `docs/fee-policy-snapshot-fields.md`, `docs/fee-policy-agent-ownership.md` |
| `1` | 정책 스키마 / 관리자 설정 백엔드 | `FeePolicyConfig.java`, `FeePolicyConfigRepository.java`, `UpdateFeePolicyRequest.java`, `FeePolicyResponse.java`, `LevelFeePolicyResponse.java`, `AdminPaymentController.java` |
| `2` | 계산 엔진 백엔드 | `FeePolicyService.java`, 신규 preview/breakdown service, 계산 관련 test |
| `3` | promo eligibility / user-level 연동 | `UserPort.java`, `OrderPort.java`, 관련 repository query, promo eligibility service |
| `4` | preview API / 결제 진입 DTO | `PaymentController.java`, preview request/response DTO, `paymentService.ts`의 preview adapter |
| `5` | payment/settlement snapshot 영속화 | `TransportPayment.java`, `PaymentLifecycleService.java`, `Settlement.java`, `SettlementResponse.java`, `TransportPaymentResponse.java` |
| `6` | 청구/지급 원장 | `FeeInvoiceService.java`, `FeeInvoiceBatchService.java`, `DriverPayoutService.java`, `FeeInvoiceItem.java`, `DriverPayoutItem.java` |
| `7` | 화주 앱 UX | `CreateOrderStep1Screen.tsx`, `CreateOrderStep2CargoScreen.tsx`, `ShipperSettlementScreen.tsx`, `feePolicy.ts` |
| `8` | 차주 앱 UX | `DriverSettlementScreen.tsx`, `CommonSettlementScreen.tsx`, 앱 `Settlement.ts` |
| `9` | 관리자 웹 UX | `app/global/settings/page.tsx`, `payment_admin_api.ts`, 관리자 정산/상세 화면 |
| `10` | QA / data migration / runbook | QA 문서, migration 문서, `PaymentE2ELabScreen.tsx` |

---

## 2. 파일 단위 상세 소유권

### 계약 문서

- owner: `에이전트 0`
- 파일:
  - `docs/shipper-fee-policy-alignment.md`
  - `docs/fee-policy-contracts.md`
  - `docs/fee-policy-snapshot-fields.md`
  - `docs/fee-policy-agent-ownership.md`

주의:

- 정책 wording 변경은 먼저 0번에서 확정한다.

### 정책 스키마 / 관리자 설정 백엔드

- owner: `에이전트 1`
- 파일:
  - `src/main/java/com/example/project/domain/payment/domain/FeePolicyConfig.java`
  - `src/main/java/com/example/project/domain/payment/repository/FeePolicyConfigRepository.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentRequest/UpdateFeePolicyRequest.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentRequest/UpdateLevelFeeRequest.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentResponse/FeePolicyResponse.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentResponse/LevelFeePolicyResponse.java`
  - `src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java`

### 계산 엔진

- owner: `에이전트 2`
- 파일:
  - `src/main/java/com/example/project/domain/payment/service/core/FeePolicyService.java`
  - `src/main/java/com/example/project/domain/payment/service/core/*Fee*Preview*Service*.java`
  - 수수료 계산 단위 테스트

주의:

- `PaymentLifecycleService.java`는 5번 owner이므로 직접 수정 전 조율

### promo eligibility / user-level 연동

- owner: `에이전트 3`
- 파일:
  - `src/main/java/com/example/project/domain/payment/port/UserPort.java`
  - `src/main/java/com/example/project/domain/payment/port/OrderPort.java`
  - `src/main/java/com/example/project/domain/payment/port/impl/JpaUserPort.java`
  - `src/main/java/com/example/project/domain/payment/port/impl/JpaOrderPort.java`
  - 관련 repository query

### preview API / 결제 진입 DTO

- owner: `에이전트 4`
- 파일:
  - `src/main/java/com/example/project/domain/payment/controller/PaymentController.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentRequest/*FeePreview*.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentResponse/*FeePreview*.java`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/api/paymentService.ts`

### payment / settlement snapshot 영속화

- owner: `에이전트 5`
- 파일:
  - `src/main/java/com/example/project/domain/payment/domain/TransportPayment.java`
  - `src/main/java/com/example/project/domain/payment/dto/paymentResponse/TransportPaymentResponse.java`
  - `src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java`
  - `src/main/java/com/example/project/domain/settlement/domain/Settlement.java`
  - `src/main/java/com/example/project/domain/settlement/dto/SettlementResponse.java`
  - `src/main/java/com/example/project/domain/settlement/service/SettlementService.java`

### 청구/지급 원장

- owner: `에이전트 6`
- 파일:
  - `src/main/java/com/example/project/domain/payment/domain/FeeInvoice.java`
  - `src/main/java/com/example/project/domain/payment/domain/FeeInvoiceItem.java`
  - `src/main/java/com/example/project/domain/payment/domain/DriverPayoutItem.java`
  - `src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceService.java`
  - `src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceBatchService.java`
  - `src/main/java/com/example/project/domain/payment/service/core/DriverPayoutService.java`
  - `src/main/java/com/example/project/domain/payment/service/query/AdminPaymentStatusQueryService.java`

### 화주 앱 UX

- owner: `에이전트 7`
- 파일:
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep1Screen.tsx`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep2CargoScreen.tsx`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/settlement/ui/ShipperSettlementScreen.tsx`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/utils/feePolicy.ts`

### 차주 앱 UX

- owner: `에이전트 8`
- 파일:
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/driver/settlement/ui/DriverSettlementScreen.tsx`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/settlement/ui/CommonSettlementScreen.tsx`
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/Settlement.ts`

### 관리자 웹 UX

- owner: `에이전트 9`
- 파일:
  - `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/settings/page.tsx`
  - `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts`
  - `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/page.tsx`
  - `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx`
  - `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`

### QA / migration / runbook

- owner: `에이전트 10`
- 파일:
  - `docs/payment-postman-test-cases.md`
  - `docs/payment-test-app-qa-checklist.md`
  - `docs/payment-env-deployment-guide.md`
  - 신규 migration/runbook 문서
  - `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx`

---

## 3. 공용 수정이 필요한 파일

아래 파일은 여러 에이전트가 동시에 건드리고 싶어질 가능성이 높다. 반드시 owner 승인 후 수정한다.

| 파일 | owner | 연관 에이전트 |
| --- | --- | --- |
| `PaymentLifecycleService.java` | `5` | `2`, `3`, `6` |
| `PaymentController.java` | `4` | `1`, `2`, `7`, `9` |
| `paymentService.ts` | `4` | `7`, `8` |
| `payment_admin_api.ts` | `9` | `1`, `4` |
| `SettlementResponse.java` | `5` | `6`, `8`, `9` |
| `FeePolicyService.java` | `2` | `1`, `4`, `5` |

---

## 4. 권장 작업 순서와 의존성

### 선행 필수

1. `에이전트 0`
2. `에이전트 1`
3. `에이전트 2`

### 그 다음 병렬 가능

- `에이전트 3`
- `에이전트 4`
- `에이전트 5`

### 원장/화면 단계

- `에이전트 6`
- `에이전트 7`
- `에이전트 8`
- `에이전트 9`

### 마지막

- `에이전트 10`

---

## 5. 충돌 방지 규칙

- 정책 필드명은 0번 문서에서만 최종 확정한다.
- entity column 추가는 1, 5, 6번이 동시에 하지 않는다.
- 앱/관리자 화면은 백엔드 DTO가 잠기기 전까지 placeholder branch에서만 작업한다.
- legacy 필드 제거는 migration 검증 이후로 미룬다.

---

## 6. 이번 문서의 결론

이번 분업의 가장 큰 리스크는 `정책 모델`, `preview DTO`, `snapshot 필드`, `화면 타입`이 각자 다른 이름으로 흩어지는 것이다.

따라서:

1. 0번이 용어와 필드명을 먼저 잠근다.
2. 1, 2, 5번이 백엔드 핵심 모델을 잠근다.
3. 7, 8, 9번은 그 계약을 그대로 소비한다.
