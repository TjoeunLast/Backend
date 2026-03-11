# 양면 수수료 정책 정리

## 문서 인덱스

기준 문서:

- [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)

구현 계약:

- [fee-policy-contracts.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-contracts.md)
- [fee-policy-snapshot-fields.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-snapshot-fields.md)
- [promotion-eligibility-rules.md](c:/ytheory/springPro/Backend/Barotruck/docs/promotion-eligibility-rules.md)

분업 문서:

- [fee-policy-agent-prompts.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-agent-prompts.md)
- [fee-policy-agent-ownership.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-agent-ownership.md)

현황 점검:

- [fee-policy-implementation-audit.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-implementation-audit.md)
- [fee-policy-cleanup-inventory.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-cleanup-inventory.md)

운영/이행 문서:

- [fee-policy-admin-transition.md](c:/ytheory/springPro/Backend/Barotruck/docs/fee-policy-admin-transition.md)
  - 상태: `구현 반영됨`
- [two-sided-fee-policy-cutover-runbook.md](c:/ytheory/springPro/Backend/Barotruck/docs/two-sided-fee-policy-cutover-runbook.md)
  - 상태: `구현 반영됨`

## 목적

화주 오더 생성부터 결제, 차주 정산까지 같은 수수료 정책을 기준으로 움직이게 한다.

이번에 기준으로 잡을 정책은 아래다.

- 화주 side 수수료율과 차주 side 수수료율은 각각 `최소 1.5%`, `최대 2.5%` 범위에서 레벨별로 적용한다.
- 결과적으로 플랫폼 총 수수료율은 거래금액 기준 `약 3% ~ 5%`가 되게 설계한다.
- 화주는 `첫 결제 프로모션`을 가진다.
- 차주는 `첫 운송 프로모션`을 가진다.
- `Toss 수수료 = 전체 거래금액의 10%`로 간주한다.
- Toss 수수료는 `가장 먼저` 차감한다.

이 문서는 위 정책을 시스템 규칙, 계산 순서, 인터페이스, 구현 변경점 기준으로 정리한다.

---

## 핵심 정책

## 1. 수수료 구조

총 거래금액을 `baseAmount`라고 할 때:

- 화주 부담 플랫폼 수수료: `shipperFee`
- 차주 부담 플랫폼 수수료: `driverFee`

둘은 같은 rate 테이블을 쓰되, 각각 별도로 계산한다.

즉 구조는 아래다.

- 화주 결제 총액 = `baseAmount`
- Toss 차감 후 기준 금액 = `baseAmount - tossFee`
- 차주 정산 기준 금액 = `postTossBaseAmount - shipperFee - driverFee`
- 플랫폼 총 수수료율 = `shipperFeeRate + driverFeeRate`
- 목표 총합 범위 = `약 3% ~ 5%`

---

## 2. 레벨별 side 수수료율

각 side 정책 범위:

- 최대: `2.5%`
- 최소: `1.5%`

권장 버킷 예시는 아래처럼 둔다.

| 레벨 버킷 | 적용 rate |
| --- | --- |
| Lv0 | 2.5% |
| Lv1 | 2.0% |
| Lv2 | 1.5% |
| Lv3+ | 1.5% |

총합 해석:

- 화주 side와 차주 side가 각각 위 범위를 가진다.
- 따라서 총 플랫폼 수수료율은 `약 3% ~ 5%` 범위를 형성한다.

설명:

- 레벨이 올라갈수록 수수료는 낮아진다.
- 현재 범위 조건이 `각 side 최대 2.5 / 최소 1.5`이므로 중간값은 `2.0`으로 두는 게 가장 단순하다.
- 필요하면 Lv3+를 `1.8`처럼 별도 조정할 수 있지만, 문서 기준 1차안은 위 표로 둔다.

---

## 3. 프로모션 규칙

### 화주

- 이름: `첫 결제 프로모션`
- 트리거: 해당 화주의 첫 결제 확정
- 적용 대상: `shipperFee`만

### 차주

- 이름: `첫 운송 프로모션`
- 트리거: 해당 차주의 첫 운송 완료 후 첫 정산 대상 생성
- 적용 대상: `driverFee`만

중요:

- 화주 프로모션은 차주 수수료에 영향을 주지 않는다.
- 차주 프로모션은 화주 수수료에 영향을 주지 않는다.
- 프로모션은 `side fee`에만 적용한다.
- `Toss 수수료`에는 프로모션을 적용하지 않는다.

권장 1차안:

- 프로모션 적용 시 해당 side fee를 `0%`로 만든다.

대안:

- 프로모션 적용 시 해당 side fee를 `최소 rate 1.5%`로 낮춘다.

운영적으로 더 강한 혜택을 원하면 `0%`, 리스크를 줄이려면 `1.5%`를 권장한다.

---

## 4. Toss 수수료 처리 순서

이 문서 기준 가장 중요한 규칙은 아래다.

1. 먼저 전체 거래금액에서 Toss 수수료를 차감한다.
2. 그 다음 남은 금액을 기준으로 화주/차주 side fee를 계산한다.
3. 마지막으로 차주 지급액을 확정한다.

즉 순서는 반드시 아래다.

1. `baseAmount` 산정
2. `tossFeeAmount = baseAmount * 10%`
3. `postTossBaseAmount = baseAmount - tossFeeAmount`
4. `shipperFeeRate` 결정
5. `driverFeeRate` 결정
6. 프로모션 반영
7. `shipperFeeAmount`, `driverFeeAmount` 계산
8. `shipperChargeAmount = baseAmount`
9. `driverPayoutAmount = postTossBaseAmount - shipperFeeAmount - driverFeeAmount`
10. `platformNetRevenue = shipperFeeAmount + driverFeeAmount`

---

## 계산식

```text
baseAmount = 거래 총액
tossFeeAmount = round(baseAmount * tossFeeRate)
postTossBaseAmount = baseAmount - tossFeeAmount

shipperFeeAmount = round(postTossBaseAmount * shipperFeeRate)
driverFeeAmount  = round(postTossBaseAmount * driverFeeRate)

shipperChargeAmount = baseAmount
driverPayoutAmount  = postTossBaseAmount - shipperFeeAmount - driverFeeAmount

platformGrossRevenue = shipperFeeAmount + driverFeeAmount
platformNetRevenue   = platformGrossRevenue
```

---

## 예시

`baseAmount = 100,000원`

`Lv0 화주`, `Lv0 차주`, 프로모션 없음이라면:

- Toss 수수료: `100,000 * 10% = 10,000원`
- Toss 차감 후 기준 금액: `90,000원`
- 화주 수수료: `90,000 * 2.5% = 2,250원`
- 차주 수수료: `90,000 * 2.5% = 2,250원`
- 화주 결제 금액: `100,000원`
- 차주 정산 금액: `90,000 - 2,250 - 2,250 = 85,500원`
- 플랫폼 총 수수료: `4,500원`
- 플랫폼 순수익: `4,500원`

---

## 설계 리스크

핵심 리스크는 `minimum fee`가 아주 작은 거래금액에서 `postTossBaseAmount`를 초과할 수 있다는 점이다.

따라서 구현에서는 아래 보정이 필요하다.

- side fee 총합이 `postTossBaseAmount`를 넘으면 총합을 `postTossBaseAmount`로 cap한다.
- 차주 지급액은 음수가 되지 않게 `0` 하한을 둔다.

---

## 권장 인터페이스

아래는 프런트/백엔드 공통 개념으로 맞추기 쉬운 최소 인터페이스다.

```ts
export type FeeLevelBucket = 0 | 1 | 2 | 3;

export type FeeSide = "SHIPPER" | "DRIVER";

export type PromotionTrigger =
  | "FIRST_SHIPPER_PAYMENT"
  | "FIRST_DRIVER_TRANSPORT";

export interface SideFeePolicy {
  minRate: number;
  maxRate: number;
  levelRates: {
    level0: number;
    level1: number;
    level2: number;
    level3Plus: number;
  };
}

export interface PromotionPolicy {
  trigger: PromotionTrigger;
  targetSide: FeeSide;
  promoRate: number;
  oncePerUser: boolean;
}

export interface TossFeePolicy {
  rate: number;
  deductFirst: true;
}

export interface MarketplaceFeePolicySnapshot {
  shipperSide: SideFeePolicy;
  driverSide: SideFeePolicy;
  shipperFirstPaymentPromo: PromotionPolicy;
  driverFirstTransportPromo: PromotionPolicy;
  toss: TossFeePolicy;
  updatedAt?: string | null;
}

export interface FeeBreakdownPreview {
  baseAmount: number;
  postTossBaseAmount: number;
  shipperAppliedLevel: FeeLevelBucket;
  driverAppliedLevel: FeeLevelBucket;
  shipperFeeRate: number;
  driverFeeRate: number;
  shipperFeeAmount: number;
  driverFeeAmount: number;
  shipperPromoApplied: boolean;
  driverPromoApplied: boolean;
  shipperChargeAmount: number;
  driverPayoutAmount: number;
  tossFeeRate: number;
  tossFeeAmount: number;
  platformGrossRevenue: number;
  platformNetRevenue: number;
}
```

---

## 화면 기준 변경 포인트

## 화주 오더 생성 화면

현재는 프런트가 `토스 10%`를 직접 계산하면 안 된다.

화주 화면에서 보여줘야 하는 값:

- 기본 운임
- 화주 side fee
- 화주 프로모션 적용 여부
- 최종 화주 청구 금액
- 안내 문구:
  - `차주 side fee는 별도 정산에서 차감됩니다.`
  - `Toss 10%를 먼저 제외한 뒤 남은 금액에서 shipper/driver side fee가 계산됩니다.`

즉 화주 화면에는 최소한 아래 필드가 필요하다.

- `shipperFeeRate`
- `shipperFeeAmount`
- `shipperPromoApplied`
- `shipperChargeAmount`

## 차주 정산 화면

차주 화면에서 보여줘야 하는 값:

- 기본 운임
- 차주 side fee
- 차주 프로모션 적용 여부
- 최종 차주 수령 예정 금액

즉 차주 화면에는 아래 필드가 필요하다.

- `driverFeeRate`
- `driverFeeAmount`
- `driverPromoApplied`
- `driverPayoutAmount`

---

## API 권장안

### 1. 프리뷰 API

오더 생성 단계에서는 프런트에서 계산하지 말고 프리뷰 API를 쓰는 것이 가장 안전하다.

추천:

- `POST /api/v1/payments/fee-preview`

요청 예시:

```json
{
  "baseAmount": 100000,
  "shipperUserId": 10,
  "driverUserId": 22,
  "paymentProvider": "TOSS"
}
```

응답 예시:

```json
{
  "baseAmount": 100000,
  "postTossBaseAmount": 90000,
  "shipperAppliedLevel": 1,
  "driverAppliedLevel": 0,
  "shipperFeeRate": 0.02,
  "driverFeeRate": 0.025,
  "shipperFeeAmount": 2000,
  "driverFeeAmount": 2000,
  "shipperPromoApplied": false,
  "driverPromoApplied": true,
  "shipperChargeAmount": 100000,
  "driverPayoutAmount": 86000,
  "tossFeeRate": 0.10,
  "tossFeeAmount": 10000,
  "platformGrossRevenue": 4000,
  "platformNetRevenue": 4000
}
```

### 2. 결제 확정 스냅샷 저장

결제 완료 시 아래 값은 반드시 스냅샷으로 저장해야 한다.

- `shipperFeeRate`
- `driverFeeRate`
- `shipperFeeAmount`
- `driverFeeAmount`
- `shipperPromoApplied`
- `driverPromoApplied`
- `tossFeeRate`
- `tossFeeAmount`
- `platformGrossRevenue`
- `platformNetRevenue`

이유:

- 나중에 레벨 정책이 바뀌어도 기존 주문 정산 근거가 흔들리면 안 된다.

---

## 구현 우선순위

### 1차

- 문서 정책 확정
- 프리뷰 인터페이스 확정
- 오더 생성 화면 하드코드 제거

### 2차

- fee preview API 추가
- 결제/정산 스냅샷 컬럼 추가
- 관리자 정책 화면에서 level rate 편집 가능하게 정리

### 3차

- 프로모션 eligibility API 추가
- 적자 Toss 주문 분석 대시보드 추가

---

## 결론

이번 기준 정책은 아래처럼 요약된다.

- 화주 side와 차주 side에 각각 `1.5% ~ 2.5%` 범위의 수수료율을 적용
- 결과적으로 플랫폼 총 수수료율은 `약 3% ~ 5%`가 되게 설계
- 화주는 첫 결제 프로모션
- 차주는 첫 운송 프로모션
- Toss 수수료 `10%`는 거래금액에서 먼저 차감
- 그 다음 남은 금액에서 shipper/driver side fee를 각각 계산

따라서 문서 기준 결론은 아래다.

1. 계산 순서는 `Toss 선차감 -> postTossBase -> side fee 계산`으로 고정한다.
2. 작은 거래금액에서는 side fee 총합을 `postTossBaseAmount` 이내로 cap한다.
