# 양면 수수료 정책 전환 Runbook

기준일: 2026-03-11

이 문서는 Barotruck 양면 수수료 정책을 운영에 안전하게 배포하기 위한 데이터 이행, QA, 관측성, 롤아웃 기준을 한 문서로 정리한 실행용 runbook이다.

관련 기준:

- `docs/shipper-fee-policy-alignment.md`
- `docs/payment-postman-test-cases.md`
- `docs/payment-test-app-qa-checklist.md`
- `docs/payment-env-deployment-guide.md`
- `FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx`

---

## 1. 현재 상태 요약

현재 구현 기준 핵심 사실:

1. 백엔드 `FeePolicyService`는 `shipperSide`, `driverSide`, `shipperFirstPaymentPromoRate`, `driverFirstTransportPromoRate`, `tossRate`를 함께 다룬다.
2. 사용자 preview와 관리자 preview는 모두 flat bilateral 계약인 `FeeBreakdownPreviewResponse`를 사용한다.
3. 현재 저장 스냅샷은 legacy alias 필드와 bilateral snapshot 필드가 함께 존재하며 `TransportPayment`가 canonical snapshot store 역할을 한다.
4. 화주 첫 결제 프로모션과 차주 첫 운송 프로모션은 각각 별도 eligibility 서비스로 판정한다.
5. 앱의 화주 오더 생성 preview는 `/api/v1/payments/fee-preview`를 사용한다.
6. payout/webhook 원장은 이미 있다.
   - `DRIVER_PAYOUT_ITEMS`
   - `PAYMENT_GATEWAY_WEBHOOK_EVENTS`
7. 테스트 앱은 화주 결제, 차주 confirm, 관리자 payout 요청/동기화까지 한 화면에서 검증할 수 있다.

정책 갭:

1. 새 정책은 화주 side fee와 차주 side fee를 분리해야 한다.
2. 화주 첫 결제 promo와 차주 첫 운송 promo를 각각 1회만 지급해야 한다.
3. Toss 수수료는 마지막에 반영되어야 한다.
4. `Toss 10%` 가정이 유지되면 일부 주문은 플랫폼 순수익이 음수가 될 수 있다.
5. legacy 주문은 bilateral snapshot이 비어 있을 수 있으므로 과거 주문 완전 복원 전략은 별도 운영 판단이 필요하다.

결론:

- 과거 주문은 “재계산”보다 “보존 + 해석”이 우선이다.
- 신규 주문부터는 양면 snapshot을 별도 불변 구조로 저장해야 한다.

---

## 2. 절대 원칙

1. 과거 주문의 기존 `TransportPayment` / `Settlement` 값을 새 정책 값으로 덮어쓰지 않는다.
2. snapshot 미보유 과거 주문을 현재 정책 테이블로 역산해 “정답처럼” 기록하지 않는다.
3. 프로모션 중복 방지는 결제 엔진의 명시적 ledger로 해결한다.
4. payout, webhook, dispute, cancel이 발생해도 최초 fee snapshot은 immutable로 유지한다.
5. 신규 정책 rollout은 feature flag 기반 단계 배포로 진행한다.

---

## 3. 레거시 주문 해석 규칙

### 3.1 레거시 주문 정의

아래 중 하나면 레거시 주문으로 본다.

1. 양면 fee snapshot이 없다.
2. promo grant ledger가 없다.
3. 주문 생성 시점의 정책 버전이 기록돼 있지 않다.

### 3.2 레거시 주문의 진실값

레거시 주문에서는 아래 값만 진실값으로 취급한다.

| 영역 | 기존 저장값 | 해석 기준 |
| --- | --- | --- |
| 결제 | `TransportPayment.amount` | 당시 화주 청구 총액으로 해석 |
| 결제 | `TransportPayment.feeRateSnapshot`, `feeAmountSnapshot` | 당시 단면 플랫폼 수수료 snapshot으로 해석 |
| 결제 | `TransportPayment.netAmountSnapshot` | 당시 차주 지급 기준 금액으로 해석 |
| 정산 | `Settlement.totalPrice`, `feeRate` | 당시 정산 원장 값으로만 사용 |
| 지급 | `DriverPayoutItem.netAmount` | 실제/예정 지급 금액으로 사용 |

주의:

- 레거시 주문의 `shipperFeeRate`, `driverFeeRate`, `tossFeeAmount`, `platformNetRevenue`는 기존 원장만으로 정확 복원되지 않을 수 있다.
- 이 값들은 비어 있으면 `null`로 남겨야 한다.

### 3.3 레거시 데이터 분류

과거 주문은 아래 3종으로 분류한다.

| 분류 | 의미 | 처리 원칙 |
| --- | --- | --- |
| `LEGACY_EXACT` | 양면 해석에 필요한 근거가 모두 남아 있음 | sidecar snapshot으로 옮기되 `source=BACKFILL_EXACT` 표기 |
| `LEGACY_PARTIAL` | 일부 금액만 있고 양면 분해가 불가 | 알 수 있는 값만 기록하고 나머지는 `null` |
| `LEGACY_UNAVAILABLE` | payment/settlement/payout 근거가 부족 | 새 snapshot 미생성, 운영 조회에서 레거시 원장만 노출 |

권장 운영 문구:

- `이 주문은 양면 정책 이전 주문입니다. 상세 side fee는 보장되지 않습니다.`

---

## 4. snapshot 미보유 과거 데이터 처리 원칙

1. 현재 `FeePolicyService` 기본값이나 최신 관리자 정책으로 과거 rate를 채우지 않는다.
2. 레거시 주문의 promo 적용 여부를 추정으로 채우지 않는다.
3. 과거 주문에 대해 필요한 것은 “새 계산 결과”가 아니라 “해석 등급”과 “조회용 연결 정보”다.
4. 레거시 주문이 dashboard 집계에 들어가야 하면 `policyVersion=LEGACY_SINGLE_SIDED`로 별도 집계한다.
5. 양면 정책 KPI와 레거시 KPI는 같은 차트에 섞지 않는다.

---

## 5. 권장 저장 구조

양면 정책은 기존 컬럼을 확장 덮어쓰기보다 sidecar snapshot + promo ledger 방식이 안전하다.

### 5.1 fee snapshot sidecar

권장 테이블 예시: `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2`

권장 컬럼:

| 컬럼 | 설명 |
| --- | --- |
| `SNAPSHOT_ID` | PK |
| `ORDER_ID` | 주문 ID |
| `PAYMENT_ID` | `TRANSPORT_PAYMENTS.PAYMENT_ID` |
| `POLICY_VERSION` | `LEGACY_SINGLE_SIDED`, `TWO_SIDED_V1` |
| `SNAPSHOT_STATUS` | `EXACT`, `PARTIAL`, `UNAVAILABLE` |
| `BASE_AMOUNT` | 운임 기준 금액 |
| `SHIPPER_LEVEL_BUCKET` | 화주 레벨 버킷 |
| `DRIVER_LEVEL_BUCKET` | 차주 레벨 버킷 |
| `SHIPPER_FEE_RATE` | 화주 side rate |
| `SHIPPER_FEE_AMOUNT` | 화주 side 금액 |
| `DRIVER_FEE_RATE` | 차주 side rate |
| `DRIVER_FEE_AMOUNT` | 차주 side 금액 |
| `SHIPPER_PROMO_APPLIED` | 화주 promo 적용 여부 |
| `DRIVER_PROMO_APPLIED` | 차주 promo 적용 여부 |
| `SHIPPER_PROMO_GRANT_ID` | 화주 promo ledger FK |
| `DRIVER_PROMO_GRANT_ID` | 차주 promo ledger FK |
| `TOSS_FEE_RATE` | Toss fee rate |
| `TOSS_FEE_AMOUNT` | Toss fee amount |
| `PLATFORM_GROSS_REVENUE` | side fee 합 |
| `PLATFORM_NET_REVENUE` | gross - toss |
| `SOURCE` | `LIVE_WRITE`, `BACKFILL_EXACT`, `BACKFILL_PARTIAL` |
| `CALCULATED_AT` | 계산 시각 |
| `NOTE` | 레거시 해석 메모 |

원칙:

- 신규 주문은 이 sidecar를 반드시 쓴다.
- 레거시 주문은 가능한 경우에만 sidecar를 채우고, 기존 `TransportPayment`는 수정하지 않는다.

### 5.2 promo grant ledger

권장 테이블 예시: `FEE_PROMOTION_GRANTS`

권장 컬럼:

| 컬럼 | 설명 |
| --- | --- |
| `GRANT_ID` | PK |
| `PROMO_TYPE` | `SHIPPER_FIRST_PAYMENT`, `DRIVER_FIRST_TRANSPORT` |
| `USER_ID` | 대상 사용자 |
| `ORDER_ID` | 해당 주문 |
| `PAYMENT_ID` | 결제 레코드 |
| `STATUS` | `RESERVED`, `CONSUMED`, `RELEASED` |
| `SOURCE` | `LIVE`, `LEGACY_HISTORY_IMPORT` |
| `CREATED_AT` | 생성 시각 |
| `CONSUMED_AT` | 소비 시각 |

필수 제약:

- `UNIQUE (PROMO_TYPE, USER_ID)` 또는 `UNIQUE (PROMO_TYPE, USER_ID, STATUS_ACTIVE)` 형태로 1회 지급 보장

이 ledger가 필요한 이유:

1. 현재 shipper promo는 count 기반이라 동시성 중복을 막기 어렵다.
2. driver first transport promo는 새로 추가되므로 명시적 1회 지급 근거가 필요하다.
3. 중복 지급 검증과 사후 감사 로그를 남길 수 있다.

---

## 6. 실제로 옮길 데이터

운영 배포 전 옮겨야 하는 데이터는 아래 3종뿐이다.

1. `FEE_PROMOTION_GRANTS` seed 데이터
2. `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2` sidecar backfill 데이터
3. rollout 기준선 비교용 집계 결과

옮기지 말아야 하는 데이터:

1. 기존 `TRANSPORT_PAYMENTS.FEE_RATE_SNAPSHOT`
2. 기존 `TRANSPORT_PAYMENTS.FEE_AMOUNT_SNAPSHOT`
3. 기존 `TRANSPORT_PAYMENTS.NET_AMOUNT_SNAPSHOT`
4. 기존 `SETTLEMENT.TOTAL_PRICE`
5. 기존 `SETTLEMENT.FEE_RATE`
6. 기존 `DRIVER_PAYOUT_ITEMS.NET_AMOUNT`

### 6.1 backfill 소스 매핑

| 신규 대상 | 주 소스 | 보조 소스 | 비고 |
| --- | --- | --- | --- |
| `FEE_PROMOTION_GRANTS` 화주 seed | `TRANSPORT_PAYMENTS` | `PAYMENT_GATEWAY_TRANSACTIONS` | `PAID`, `CONFIRMED`, `ADMIN_FORCE_CONFIRMED` 기준 |
| `FEE_PROMOTION_GRANTS` 차주 seed | `TRANSPORT_PAYMENTS` | `DRIVER_PAYOUT_ITEMS` | confirmed 또는 payout 존재 기준 |
| `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2.BASE_AMOUNT` | 주문 snapshot / `SETTLEMENT.TOTAL_PRICE` | `TRANSPORT_PAYMENTS` | 근거가 불일치하면 `NOTE`에 기록 |
| `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2.SHIPPER_*` | `TRANSPORT_PAYMENTS` | 관리자 정책 이력 | 레거시는 exact 불가 시 `null` 허용 |
| `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2.DRIVER_*` | `TRANSPORT_PAYMENTS.NET_AMOUNT_SNAPSHOT` | `DRIVER_PAYOUT_ITEMS.NET_AMOUNT` | rate 역산이 애매하면 amount만 기록 |
| `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2.TOSS_*` | `PAYMENT_GATEWAY_TRANSACTIONS` | webhook payload | legacy는 없는 경우가 많음 |
| `TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2.NOTE` | backfill 스크립트 | 운영 메모 | 해석 근거를 반드시 남김 |

---

## 7. backfill 절차

### 7.1 사전 준비

1. 신규 schema를 먼저 배포하되 모든 v2 flag는 `OFF`로 둔다.
2. backfill 스크립트는 dry-run 모드를 기본으로 둔다.
3. 실행 전 기준 시각 `cutover_at`을 고정한다.
4. 기준 시각 이전 주문만 backfill 대상으로 삼는다.

### 7.2 후보 주문 추출

기준:

1. `TRANSPORT_PAYMENTS`가 있는 주문
2. 또는 `SETTLEMENT` / `DRIVER_PAYOUT_ITEMS`가 있는 완료 주문
3. 주문 상태가 `COMPLETED`, `PAID`, `CONFIRMED`, `DISPUTED`, `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED` 인 주문 우선

권장 점검 쿼리:

```sql
SELECT COUNT(*) AS payment_count
FROM TRANSPORT_PAYMENTS
WHERE CREATED_AT < :cutover_at;

SELECT STATUS, COUNT(*)
FROM TRANSPORT_PAYMENTS
WHERE CREATED_AT < :cutover_at
GROUP BY STATUS;
```

### 7.3 promo ledger seed

#### 화주 first payment

원칙:

1. `cutover_at` 이전에 `PAID`, `CONFIRMED`, `ADMIN_FORCE_CONFIRMED` 결제가 1건 이상 있으면 promo는 이미 소진된 것으로 본다.
2. earliest paid/confirmed payment를 기준으로 `FEE_PROMOTION_GRANTS`를 `CONSUMED`로 seed한다.
3. 실제 과거 promo 적용 여부가 불명확해도 post-cutover 중복 지급을 막기 위해 보수적으로 소진 처리한다.
4. 과거 누락 혜택 보상은 결제 엔진이 아니라 별도 운영 프로모션으로 처리한다.

#### 차주 first transport

원칙:

1. `cutover_at` 이전에 `CONFIRMED`, `ADMIN_FORCE_CONFIRMED` 결제가 있거나 `DRIVER_PAYOUT_ITEMS`가 존재하면 promo는 소진된 것으로 본다.
2. earliest confirmed/payout order 기준으로 `CONSUMED` seed를 넣는다.
3. 같은 driver에게 2개 이상 seed가 생기면 실패 처리한다.

필수 검증:

```sql
SELECT PROMO_TYPE, USER_ID, COUNT(*)
FROM FEE_PROMOTION_GRANTS
GROUP BY PROMO_TYPE, USER_ID
HAVING COUNT(*) > 1;
```

### 7.4 fee snapshot sidecar backfill

규칙:

1. 신규 정책으로 재산출 가능한 근거가 있어도 `source=BACKFILL_EXACT` 또는 `BACKFILL_PARTIAL`로만 기록한다.
2. 레거시 단면 원장만 있는 주문은 `POLICY_VERSION=LEGACY_SINGLE_SIDED`로 저장한다.
3. 양면 rate/amount를 확정할 수 없으면 `null`로 둔다.
4. `NOTE`에 해석 근거를 남긴다.
   - 예: `legacy transport_payment only`
   - 예: `legacy payment + payout, driver side exact unknown`

### 7.5 backfill 완료 검증

필수 검증 항목:

1. 대상 주문 수와 sidecar 생성 수가 분류 기준과 맞는지 확인
2. 중복 promo grant가 없는지 확인
3. `LEGACY_PARTIAL` 비율이 예상보다 급증하지 않았는지 확인
4. 기존 `TRANSPORT_PAYMENTS` / `SETTLEMENT` 금액이 변경되지 않았는지 diff 확인

권장 검증 쿼리:

```sql
SELECT POLICY_VERSION, SNAPSHOT_STATUS, COUNT(*)
FROM TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2
GROUP BY POLICY_VERSION, SNAPSHOT_STATUS;

SELECT COUNT(*)
FROM TRANSPORT_PAYMENT_FEE_SNAPSHOT_V2
WHERE SOURCE LIKE 'BACKFILL%'
  AND CALCULATED_AT IS NULL;
```

---

## 8. 신규 주문 write 규칙

cutover 이후 신규 주문에는 아래 규칙을 적용한다.

1. fee preview 단계에서 양면 fee 계산
2. 결제 확정 시점에 양면 snapshot을 immutable로 저장
3. promo는 ledger reserve/consume 방식으로 단건 보장
4. `Settlement`, `DriverPayoutItem`는 v2 snapshot을 읽어 표시하되 레거시 필드는 하위 호환으로 유지
5. cancel, dispute, payout retry가 발생해도 v2 snapshot 본문은 수정하지 않는다

---

## 9. QA 시나리오

기존 문서의 Postman 케이스(`TC-01`~`TC-14`)와 테스트 앱 체크리스트(`RUN-01`~`RUN-14`)는 그대로 유지하고, 아래 v2 시나리오를 추가한다.

| ID | 구분 | 시나리오 | 기대 결과 |
| --- | --- | --- | --- |
| V2-01 | 계산 | 신규 주문, 프로모션 없음 | shipper/driver/toss/net revenue가 모두 snapshot 저장 |
| V2-02 | 계산 | 화주 첫 결제 promo | shipper promo만 적용, driver promo는 false |
| V2-03 | 계산 | 차주 첫 운송 promo | driver promo만 적용, shipper promo는 false |
| V2-04 | 계산 | 양쪽 promo 동시 적용 가능 케이스 | 두 promo가 독립적으로 기록되고 각 grant id가 다름 |
| V2-05 | 계산 | Toss 결제 negative margin | `platformNetRevenue < 0` 저장, alert 후보로 집계 |
| V2-06 | 멱등 | 같은 shipper가 중복 confirm/재시도 | 같은 promo grant 재사용, 추가 grant 미생성 |
| V2-07 | 멱등 | 같은 driver가 첫 운송 promo 대상 주문을 두 번 confirm | driver promo grant 1건만 존재 |
| V2-08 | 레거시 | 양면 snapshot 없는 과거 주문 조회 | 레거시 원장 노출, v2 snapshot은 `null` 또는 `PARTIAL` |
| V2-09 | 취소 | prepare 후 취소 | v2 snapshot 미소비 또는 `RELEASED`, payout 미생성 |
| V2-10 | 취소 | 결제 완료 후 취소, payout 미생성 | 기존 cancel 규칙 유지, 과거 snapshot 불변 |
| V2-11 | 분쟁 | `DISPUTED` / `ADMIN_HOLD` 전환 | settlement가 `WAIT`로 가도 fee snapshot은 유지 |
| V2-12 | payout | `payout.changed REQUESTED` | payout item이 `REQUESTED`, webhook 반영 일시 기록 |
| V2-13 | payout | `payout.changed COMPLETED` | payout item `COMPLETED`, `feeCompleteDate` 반영 |
| V2-14 | stale webhook | 완료 후 실패 webhook | 기존 `COMPLETED` 유지, 경고 로그만 남김 |
| V2-15 | seller | `seller.changed` | `sellerStatus` 반영, payout 상태와 별도 추적 |
| V2-16 | rollback | flag off 후 신규 주문 생성 | legacy 계산 경로로 복귀, 기존 v2 데이터 읽기는 유지 |

### 9.1 first promo 중복 지급 검증

반드시 검증할 것:

1. 동일 shipper에 대해 `SHIPPER_FIRST_PAYMENT` grant가 2건 생기지 않는지
2. 동일 driver에 대해 `DRIVER_FIRST_TRANSPORT` grant가 2건 생기지 않는지
3. prepare 재호출, confirm 재시도, webhook 재전송이 와도 grant 상태가 보존되는지
4. cancel된 주문의 reserved grant가 release되는지
5. 이미 legacy seed가 들어간 사용자에게 신규 grant가 생기지 않는지

### 9.2 Payment E2E Lab 활용 범위

`PaymentE2ELabScreen` 기준으로 아래 조합을 staging 직전까지 최소 1회씩 수행한다.

1. 화주: billing context, agreement 조회, `Toss prepare`, `mark-paid`
2. 차주: `driver confirm`, settlement 상태, payout 상태
3. 관리자: payout request, payout sync, payout item 조회, batch status 조회

주의:

- `payment.test-support.enabled` 기반 snapshot API는 local/dev 전용이다.
- staging/prod 검증은 일반 API, 관리자 API, webhook 원장 조회 기준으로 수행한다.

### 9.3 실행에 사용할 API

| 목적 | API |
| --- | --- |
| 화주 manual paid | `POST /api/v1/payments/orders/{orderId}/mark-paid` |
| 차주 confirm | `POST /api/v1/payments/orders/{orderId}/confirm` |
| Toss prepare | `POST /api/v1/payments/orders/{orderId}/toss/prepare` |
| Toss confirm | `POST /api/v1/payments/orders/{orderId}/toss/confirm` |
| Toss webhook | `POST /api/v1/payments/webhooks/toss` |
| 관리자 payout request | `POST /api/admin/payment/orders/{orderId}/payouts/request` |
| 관리자 payout sync | `POST /api/admin/payment/payout-items/orders/{orderId}/sync` |
| 관리자 payout item status | `GET /api/admin/payment/payout-items/orders/{orderId}/status` |
| 관리자 payout batch status | `GET /api/admin/payment/payouts/status?date=YYYY-MM-DD` |
| local/dev 테스트 snapshot | `GET /api/test/payment-scenarios/orders/{orderId}/snapshot` |

---

## 10. 로그, 메트릭, 대시보드 요구사항

### 10.1 구조화 로그

필수 로그 이벤트:

| 이벤트명 | 시점 | 필수 필드 |
| --- | --- | --- |
| `fee_policy_v2_preview_computed` | preview 계산 | `orderId`, `shipperUserId`, `driverUserId`, `policyVersion`, `baseAmount`, `shipperFeeAmount`, `driverFeeAmount`, `tossFeeAmount`, `platformNetRevenue` |
| `fee_policy_v2_snapshot_written` | snapshot 저장 | `paymentId`, `snapshotId`, `promoGrantIds`, `source`, `snapshotStatus` |
| `fee_promo_grant_reserved` | promo 예약 | `promoType`, `userId`, `orderId` |
| `fee_promo_grant_consumed` | promo 소비 | `promoType`, `userId`, `orderId`, `grantId` |
| `fee_promo_grant_duplicate_blocked` | 중복 차단 | `promoType`, `userId`, `orderId`, `existingGrantId` |
| `fee_policy_legacy_interpreted` | 레거시 조회 | `orderId`, `policyVersion`, `snapshotStatus`, `note` |
| `negative_margin_toss_order_detected` | net revenue < 0 | `orderId`, `paymentId`, `platformNetRevenue`, `shipperPromoApplied`, `driverPromoApplied` |
| `payout_webhook_stale_ignored` | stale failure 무시 | `orderId`, `payoutRef`, `webhookId`, `currentStatus` |

### 10.2 메트릭

필수 메트릭:

1. `fee_policy_v2_snapshot_write_total{status,source}`
2. `fee_policy_v2_negative_margin_total{provider,policy_version}`
3. `fee_policy_v2_negative_margin_amount_sum{provider,policy_version}`
4. `fee_promo_grant_total{promo_type,status}`
5. `fee_promo_duplicate_block_total{promo_type}`
6. `payment_webhook_unprocessed_total{event_type}`
7. `payout_status_total{status}`
8. `payout_webhook_mismatch_total`
9. `legacy_partial_snapshot_total`

### 10.3 대시보드

운영 대시보드는 최소 아래 패널을 가진다.

1. 신규 주문 수 대비 v2 snapshot 생성률
2. policy version별 주문 수
3. promo type별 지급 건수 / 차단 건수
4. Toss negative margin 주문 건수 / 총액 / 최근 24시간 추이
5. payout status 분포
6. webhook event type별 수신량, 처리 지연, 실패 건수
7. `webhookMatchesPayoutStatus=false` 건수
8. `LEGACY_PARTIAL` / `LEGACY_UNAVAILABLE` 주문 비율

### 10.4 알람

운영 알람 기준:

| 알람 | 기준 | 심각도 |
| --- | --- | --- |
| promo 중복 grant | 1건 이상 | Critical |
| 신규 cohort 주문의 v2 snapshot 누락 | 1건 이상 | Critical |
| Toss negative margin 주문 | 15분 내 1건 이상 | Warning |
| Toss negative margin 총액 급증 | 1시간 합계 임계치 초과 | Critical |
| webhook 미처리 backlog | `processedAt is null` 5분 초과 | Warning |
| payout/webhook 상태 불일치 | 3건 이상 지속 | Warning |
| payout failed 급증 | 1시간 내 실패율 임계치 초과 | Critical |

권장 메모:

- negative margin 알람은 “장애”가 아니라 “정책 손익 이상” 탐지다.
- 따라서 장애 알람과 별도 채널로 분리한다.

---

## 11. 단계별 배포 전략

권장 feature flag:

1. `payment.fee-policy-v2.preview-enabled`
2. `payment.fee-policy-v2.snapshot-write-enabled`
3. `payment.fee-policy-v2.shipper-promo-ledger-enabled`
4. `payment.fee-policy-v2.driver-promo-ledger-enabled`
5. `payment.fee-policy-v2.read-enabled`
6. `payment.fee-policy-v2.negative-margin-alert-enabled`
7. `payment.fee-policy-v2.allowlist-enabled`

### Stage 0. Schema only

1. 신규 table/column 배포
2. 모든 flag `OFF`
3. backfill dry-run만 수행

### Stage 1. Shadow preview

1. `preview-enabled`만 `ON`
2. 신규 계산값을 로그/metric에만 남기고 사용자 금액은 바꾸지 않음
3. negative margin 분포를 먼저 측정

### Stage 2. Snapshot write for internal allowlist

1. `snapshot-write-enabled`, promo ledger flags `ON`
2. allowlist 사용자만 대상
3. 신규 snapshot 저장과 중복 promo 방지만 먼저 검증

### Stage 3. Staging full E2E

1. Postman `TC-01`~`TC-14`
2. V2-01 ~ V2-16
3. Payment E2E Lab `RUN-01`~`RUN-14`
4. webhook 재전송, stale failure, payout sync까지 포함

### Stage 4. Prod canary

1. prod allowlist 또는 신규 주문 1% cohort
2. 24시간 관찰
3. negative margin, promo duplicate, snapshot 누락 0건 확인

### Stage 5. Prod 확대

1. 10% -> 50% -> 100%
2. 각 단계 사이 최소 1일 관찰
3. 문제 발생 시 `read-enabled`만 유지하고 write/allowlist flag를 먼저 내림

### Rollback 원칙

1. 신규 주문 계산/write flag부터 즉시 `OFF`
2. 이미 생성된 v2 snapshot과 promo ledger는 삭제하지 않음
3. 기존 legacy read path는 계속 유지
4. rollback 중에도 duplicate grant 알람은 계속 켠다

---

## 12. 운영 체크리스트

### 12.1 배포 전

- [ ] v2 schema 배포 완료
- [ ] promo ledger unique 제약 확인
- [ ] backfill dry-run 결과 검토 완료
- [ ] duplicate grant 0건 확인
- [ ] `LEGACY_PARTIAL` 비율이 허용 범위인지 검토
- [ ] staging Postman / E2E Lab / v2 QA pass
- [ ] negative margin dashboard 생성
- [ ] webhook backlog / payout mismatch dashboard 생성
- [ ] `payment.test-support.enabled`가 prod에서 꺼져 있는지 확인
- [ ] feature flag 기본값이 모두 `OFF`인지 확인

### 12.2 배포 직후

- [ ] canary cohort 주문 1건 이상 생성 확인
- [ ] v2 snapshot write 성공 확인
- [ ] shipper promo 중복 0건 확인
- [ ] driver promo 중복 0건 확인
- [ ] payout request / sync / webhook 반영 정상 확인
- [ ] negative margin 탐지 로그가 정상 집계되는지 확인

### 12.3 배포 후 24시간

- [ ] snapshot 누락 0건
- [ ] duplicate grant 0건
- [ ] stale payout failure 무시 로그 정상
- [ ] `webhookMatchesPayoutStatus=false` 지속 건수 확인
- [ ] cohort 확장 여부 결정

---

## 13. Go / No-Go 기준

Go 조건:

1. backfill이 기존 원장을 변경하지 않았다.
2. promo duplicate가 0건이다.
3. staging에서 cancel, dispute, failed payout, retry, stale webhook 케이스가 모두 통과했다.
4. negative margin 모니터링이 live로 보인다.
5. rollout flag가 단계별로 분리돼 있다.

No-Go 조건:

1. 신규 cohort에서 v2 snapshot 누락이 1건이라도 발생
2. promo grant 중복이 1건이라도 발생
3. payout completed 이후 stale failure가 상태를 덮어씀
4. `WAIT` settlement가 webhook으로 잘못 `COMPLETED` 처리됨
5. negative margin 알람이 보이지 않거나 집계가 비정상

---

## 14. 최종 결론

운영 배포 전 반드시 실행해야 하는 일은 명확하다.

1. 기존 단면 원장은 그대로 보존한다.
2. 양면 정책은 sidecar snapshot과 promo ledger로 신규 구조를 추가한다.
3. 과거 주문은 `LEGACY_EXACT` / `LEGACY_PARTIAL` / `LEGACY_UNAVAILABLE`로 해석만 남긴다.
4. shipper/driver first promo는 명시적 ledger와 unique 제약으로 중복을 막는다.
5. Toss negative margin 주문은 별도 로그, 메트릭, 대시보드, 알람으로 감시한다.
6. rollout은 flag 기반 shadow -> allowlist -> canary -> 확대 순서로 진행한다.

이 원칙을 지키면 운영 데이터 위조 없이 양면 수수료 정책으로 안전하게 전환할 수 있다.
