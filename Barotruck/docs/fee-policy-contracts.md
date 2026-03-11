# 양면 수수료 정책 계약 문서

기준일: 2026-03-11

이 문서는 [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)를 실제 구현 계약으로 고정한다.

목표는 세 가지다.

1. 백엔드, 모바일 앱, 관리자 웹이 같은 필드명을 쓴다.
2. preview 계산과 실제 결제/정산 snapshot이 같은 숫자를 쓴다.
3. 기존 단면 수수료 구조를 양면 수수료 구조로 확장하되, 전환 경로를 명확히 남긴다.

---

## 1. 핵심 원칙

- 화주 side 수수료율과 차주 side 수수료율은 각각 `1.5% ~ 2.5%` 범위에서 레벨별로 적용한다.
- 결과적으로 플랫폼 총 수수료율은 거래금액 기준 `약 3% ~ 5%`가 되게 설계한다.
- 화주 `첫 결제 프로모션`과 차주 `첫 운송 프로모션`은 서로 독립이다.
- `Toss 수수료`는 `baseAmount` 기준으로 가장 먼저 계산한다.
- 사용자 화면은 각자 자기 side 금액을 중심으로 본다.
- `TransportPayment`가 결제 기준 snapshot의 canonical store다.
- `Settlement`, `FeeInvoiceItem`, `DriverPayoutItem`는 목적별 복제/파생 필드만 가진다.

---

## 2. 용어 표준

### 금액 기준

- `baseAmount`
  - 화주 결제 총액
  - Toss 차감 전 거래 총액

- `postTossBaseAmount`
  - Toss 차감 후 남은 금액
  - shipper/driver side fee 계산의 기준값

- `shipperChargeAmount`
  - 화주가 실제로 결제하는 총액
  - `baseAmount`

- `driverPayoutAmount`
  - 차주가 실제로 수령하는 금액
  - `postTossBaseAmount - shipperFeeAmount - driverFeeAmount`

### rate 기준

- `shipperFeeRate`
  - 화주 side에 적용된 플랫폼 수수료율

- `driverFeeRate`
  - 차주 side에 적용된 플랫폼 수수료율

- `tossFeeRate`
  - Toss 수수료율
  - 항상 첫 계산에만 사용한다.

### promo 기준

- `shipperPromoEligible`
  - 화주 첫 결제 프로모션 eligibility 판정 결과

- `driverPromoEligible`
  - 차주 첫 운송 프로모션 eligibility 판정 결과

- `shipperPromoApplied`
  - 실제 계산에서 화주 promo rate가 적용되었는지

- `driverPromoApplied`
  - 실제 계산에서 차주 promo rate가 적용되었는지

### 수익 기준

- `platformGrossRevenue`
  - `shipperFeeAmount + driverFeeAmount`

- `platformNetRevenue`
  - `platformGrossRevenue`

- `negativeMargin`
  - `platformNetRevenue < 0`

---

## 3. Canonical 필드명

아래 필드명이 최종 표준이다.

| 필드명 | 타입 | 의미 | 비고 |
| --- | --- | --- | --- |
| `baseAmount` | decimal(18,2) | 거래 총액 | 화주 결제 총액 |
| `postTossBaseAmount` | decimal(18,2) | Toss 차감 후 기준 금액 | side fee 계산 기준 |
| `shipperAppliedLevel` | int | 화주 적용 레벨 버킷 | `0,1,2,3` |
| `driverAppliedLevel` | int \| null | 차주 적용 레벨 버킷 | 미배정 preview면 null 가능 |
| `shipperFeeRate` | decimal(6,4) | 화주 side 수수료율 | |
| `driverFeeRate` | decimal(6,4) \| null | 차주 side 수수료율 | driver unresolved면 null 가능 |
| `shipperFeeAmount` | decimal(18,2) | 화주 side 수수료 금액 | |
| `driverFeeAmount` | decimal(18,2) \| null | 차주 side 수수료 금액 | |
| `shipperPromoEligible` | boolean | 화주 promo eligibility | preview/admin 중심 |
| `driverPromoEligible` | boolean \| null | 차주 promo eligibility | |
| `shipperPromoApplied` | boolean | 화주 promo 실제 적용 여부 | snapshot 필수 |
| `driverPromoApplied` | boolean \| null | 차주 promo 실제 적용 여부 | snapshot 필수 |
| `shipperChargeAmount` | decimal(18,2) | 화주 결제 총액 | `baseAmount`와 동일 |
| `driverPayoutAmount` | decimal(18,2) \| null | 차주 수령 금액 | |
| `tossFeeRate` | decimal(6,4) | Toss 수수료율 | non-Toss도 정책 응답엔 포함 가능 |
| `tossFeeAmount` | decimal(18,2) | Toss 수수료 금액 | non-Toss는 `0.00` 허용 |
| `platformGrossRevenue` | decimal(18,2) | side fee 총합 | |
| `platformNetRevenue` | decimal(18,2) | 플랫폼 순수익 | 현 정책에서는 `platformGrossRevenue`와 동일 |
| `negativeMargin` | boolean | 손익 음수 여부 | |
| `policyConfigId` | long | 적용 정책 row 식별자 | snapshot 필수 |
| `policyUpdatedAt` | datetime | 정책 생성/업데이트 시각 | snapshot 필수 |

---

## 4. Legacy 필드와의 매핑

현재 단면 구조의 핵심 문제는 아래 4개 필드가 실제 의미보다 너무 좁거나 모호하다는 점이다.

현재 필드:

- `TransportPayment.amount`
- `TransportPayment.feeRateSnapshot`
- `TransportPayment.feeAmountSnapshot`
- `TransportPayment.netAmountSnapshot`

이 필드들은 아래처럼 해석을 고정한다.

| 현재 필드 | 현재 실제 의미 | 계약상 canonical 의미 |
| --- | --- | --- |
| `amount` | 화주 청구 총액 | `shipperChargeAmount` |
| `feeRateSnapshot` | 화주 side 수수료율 | `shipperFeeRate` |
| `feeAmountSnapshot` | 화주 side 수수료 금액 | `shipperFeeAmount` |
| `netAmountSnapshot` | 차주 실수령 금액 | `driverPayoutAmount` |

중요:

- 위 4개 필드는 앞으로 `총 플랫폼 fee`나 `driver fee` 의미로 재사용하면 안 된다.
- 양면 구조 전환 후에는 명시적 bilateral 필드로 확장해야 한다.
- 레거시 필드는 DB migration 동안 공존할 수 있지만, DTO와 서비스 계약에서는 canonical 명칭을 우선한다.

---

## 5. 정책 스키마 계약

정책 조회/수정 API는 flat DTO보다 `side 분리형 정책 모델`을 canonical로 사용한다.

```ts
export type FeeLevelBucket = 0 | 1 | 2 | 3;

export interface SideFeePolicyContract {
  minRate: number;
  maxRate: number;
  level0Rate: number;
  level1Rate: number;
  level2Rate: number;
  level3PlusRate: number;
}

export interface PromotionPolicyContract {
  enabled: boolean;
  promoRate: number;
  oncePerUser: boolean;
  trigger: "FIRST_SHIPPER_PAYMENT" | "FIRST_DRIVER_TRANSPORT";
}

export interface TossFeePolicyContract {
  rate: number;
  deductFirst: true;
}

export interface MarketplaceFeePolicyContract {
  policyConfigId: number;
  shipperSide: SideFeePolicyContract;
  driverSide: SideFeePolicyContract;
  shipperFirstPaymentPromo: PromotionPolicyContract;
  driverFirstTransportPromo: PromotionPolicyContract;
  toss: TossFeePolicyContract;
  updatedAt: string | null;
}
```

정책 편집 검증 규칙:

- `shipperSide.level*Rate`, `driverSide.level*Rate`는 `0.015 <= rate <= 0.025`
- 레벨이 올라갈수록 같은 side의 rate가 증가하면 안 된다.
- `promoRate`는 `0 <= promoRate <= side.maxRate`
- `toss.rate`는 `0 <= rate <= 1`
- `toss.deductFirst`는 항상 `true`

---

## 6. Fee Preview API 계약

### path

- `POST /api/v1/payments/fee-preview`

### 권한 규칙

- `SHIPPER`
  - 자기 자신 기준 preview만 허용
  - `shipperUserId` override 불가
  - `shipperLevelOverride`, `driverLevelOverride`, promo override 불가

- `ADMIN`
  - 임의 사용자, 레벨, promo 상황 시뮬레이션 허용

### request

```json
{
  "previewMode": "ORDER_CREATE",
  "baseAmount": 100000,
  "paymentProvider": "TOSS",
  "orderId": null,
  "shipperUserId": null,
  "driverUserId": null,
  "shipperLevelOverride": null,
  "driverLevelOverride": null,
  "shipperPromoEligibleOverride": null,
  "driverPromoEligibleOverride": null
}
```

### request 해석 규칙

- `baseAmount`는 필수
- `paymentProvider`는 필수
- `orderId`가 있으면 가능한 participant를 order에서 우선 resolve
- `SHIPPER` 요청에서는 `shipperUserId`를 무시하고 인증 사용자 기준으로 강제
- `driverUserId`가 없으면 `driverAppliedLevel`, `driverFeeRate`, `driverFeeAmount`, `driverPayoutAmount`는 null 가능
- admin simulation에서만 level/promo override 허용

### response

```json
{
  "previewMode": "ORDER_CREATE",
  "paymentProvider": "TOSS",
  "baseAmount": 100000,
  "postTossBaseAmount": 90000,
  "shipperAppliedLevel": 1,
  "driverAppliedLevel": null,
  "shipperFeeRate": 0.02,
  "driverFeeRate": null,
  "shipperFeeAmount": 2000,
  "driverFeeAmount": null,
  "shipperPromoEligible": false,
  "driverPromoEligible": null,
  "shipperPromoApplied": false,
  "driverPromoApplied": null,
  "shipperChargeAmount": 100000,
  "driverPayoutAmount": null,
  "tossFeeRate": 0.10,
  "tossFeeAmount": 10000,
  "platformGrossRevenue": 2000,
  "platformNetRevenue": 2000,
  "negativeMargin": false,
  "policyConfigId": 12,
  "policyUpdatedAt": "2026-03-11T09:00:00"
}
```

### response 규칙

- 응답은 flat DTO를 canonical로 사용한다.
- create-order 단계에서는 `driver*` 필드가 일부 null일 수 있다.
- non-Toss 결제라도 `tossFeeRate`는 정책값을 포함할 수 있으나, `tossFeeAmount`는 `0.00`으로 내려도 된다.
- 관리자 웹은 이 응답만으로 시뮬레이터를 구성할 수 있어야 한다.

---

## 7. 결제/정산 Snapshot 계약

### canonical snapshot store

- `TransportPayment`

### canonical store에 반드시 남길 값

- participant
  - `orderId`
  - `shipperUserId`
  - `driverUserId`

- money
  - `baseAmount`
  - `shipperChargeAmount`
  - `driverPayoutAmount`
  - `shipperFeeAmount`
  - `driverFeeAmount`
  - `tossFeeAmount`
  - `platformGrossRevenue`
  - `platformNetRevenue`

- rate
  - `shipperFeeRate`
  - `driverFeeRate`
  - `tossFeeRate`

- level/promo
  - `shipperAppliedLevel`
  - `driverAppliedLevel`
  - `shipperPromoApplied`
  - `driverPromoApplied`

- policy
  - `policyConfigId`
  - `policyUpdatedAt`

- meta
  - `paymentProvider`
  - `paymentMethod`
  - `paymentTiming`
  - `negativeMargin`

### downstream 원칙

- `Settlement`는 workflow와 화면 조회 최적화용이다.
- `FeeInvoiceItem`는 화주 side 과금 원장이다.
- `DriverPayoutItem`는 차주 side 지급 원장이다.
- downstream 객체는 자기 도메인에 필요한 bilateral subset만 복제한다.

---

## 8. 프로모션 eligibility 계약

### 화주 첫 결제 프로모션

- business trigger
  - 화주의 첫 유효 결제

- technical freeze point
  - `TransportPayment`를 `PAID`로 만들기 직전

- eligibility rule
  - 같은 `shipperUserId`에 대해 상태가 아래 중 하나인 이전 결제 건이 0건이어야 한다.
  - `PAID`
  - `CONFIRMED`
  - `ADMIN_FORCE_CONFIRMED`

### 차주 첫 운송 프로모션

- business trigger
  - 차주의 첫 정산 대상 운송

- technical freeze point
  - driver-side payout snapshot 또는 첫 `DriverPayoutItem` 생성 직전

- eligibility rule
  - 같은 `driverUserId`에 대해 이미 생성된 payout 대상 건이 0건이어야 한다.
  - cancelled/no-op 건은 제외 가능하지만, 동일 driver에 대해 이미 유효 payout 대상이 있으면 promo 불가

중요:

- promo eligibility와 promo applied는 다른 필드다.
- override 없이 runtime에서 다시 계산하지 말고, snapshot에 `promoApplied`를 남겨야 한다.

---

## 9. 손익 음수 주문 표시 기준

관리자 화면과 운영 로깅에서 손익 음수 주문은 아래 규칙으로 고정한다.

- `negativeMargin = platformNetRevenue < 0`
- `platformNetRevenue == 0`은 음수 주문이 아니다.
- `paymentProvider == TOSS`이고 `negativeMargin == true`인 주문은 `NEGATIVE_MARGIN_TOSS` 운영 태그를 붙인다.

관리자 기본 표시 요구:

- 주문 상세
  - `platformGrossRevenue`
  - `tossFeeAmount`
  - `platformNetRevenue`
  - `negativeMargin`

- 목록 필터
  - `negativeMarginOnly`

---

## 10. 화면별 노출 계약

### 화주 앱

필수 노출:

- `baseAmount`
- `postTossBaseAmount`
- `shipperFeeRate`
- `shipperFeeAmount`
- `shipperPromoApplied`
- `shipperChargeAmount`

안내 문구:

- `차주 side fee는 별도 정산에서 차감됩니다.`
- `Toss 10%를 먼저 제외한 뒤 남은 금액에서 shipper/driver side fee가 계산됩니다.`

### 차주 앱

필수 노출:

- `baseAmount`
- `driverFeeRate`
- `driverFeeAmount`
- `driverPromoApplied`
- `driverPayoutAmount`

### 관리자 웹

필수 노출:

- 양 side 수수료율/금액
- toss 수수료율/금액
- gross/net revenue
- negative margin 여부

---

## 11. 이번 계약의 결론

이번 구현에서 반드시 지켜야 할 고정점은 아래다.

1. preview DTO는 flat bilateral breakdown으로 고정한다.
2. 정책 편집 DTO는 side 분리형 구조로 고정한다.
3. `TransportPayment`를 canonical fee snapshot store로 사용한다.
4. legacy 단면 필드는 bilateral canonical 의미로만 해석한다.
5. `Toss 선차감 -> postTossBase -> side fee 계산` 규칙은 예외 없이 유지한다.
