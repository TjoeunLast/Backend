# 양면 수수료 정책 Snapshot 필드 명세

기준일: 2026-03-11

이 문서는 어떤 엔티티가 어떤 bilateral fee snapshot을 가져야 하는지 고정한다.

핵심 원칙:

- full bilateral snapshot의 canonical store는 `TransportPayment`
- `Settlement`, `FeeInvoiceItem`, `DriverPayoutItem`는 목적별 subset만 가진다.
- 과거 주문 재현은 `TransportPayment` snapshot만으로 가능해야 한다.

---

## 1. 계층 구분

### A. Canonical Snapshot

- 대상: `TRANSPORT_PAYMENTS`
- 책임: 결제 당시 bilateral fee 계산 결과 전부 저장

### B. Operational Snapshot

- 대상: `SETTLEMENT`
- 책임: workflow/status 중심 조회와 운영 패널 최적화

### C. Billing Snapshot

- 대상: `FEE_INVOICE_ITEMS`, `FEE_INVOICES`
- 책임: 화주 side fee 원장

### D. Payout Snapshot

- 대상: `DRIVER_PAYOUT_ITEMS`
- 책임: 차주 side 지급 원장

---

## 2. TRANSPORT_PAYMENTS 필수 필드

현재 존재:

- `ORDER_ID`
- `SHIPPER_USER_ID`
- `DRIVER_USER_ID`
- `AMOUNT`
- `FEE_RATE_SNAPSHOT`
- `FEE_AMOUNT_SNAPSHOT`
- `NET_AMOUNT_SNAPSHOT`
- `METHOD`
- `PAYMENT_TIMING`
- `STATUS`
- `PG_TID`
- `PROOF_URL`
- `PAID_AT`
- `CONFIRMED_AT`

양면 정책 기준 필수 추가/변경 필드:

| 필드명 | 필수 | 설명 |
| --- | --- | --- |
| `BASE_AMOUNT` | Y | 운임 기준 금액 |
| `SHIPPER_APPLIED_LEVEL` | Y | 화주 적용 레벨 |
| `DRIVER_APPLIED_LEVEL` | Y | 차주 적용 레벨 |
| `SHIPPER_FEE_RATE_SNAPSHOT` | Y | 화주 side 수수료율 |
| `DRIVER_FEE_RATE_SNAPSHOT` | Y | 차주 side 수수료율 |
| `SHIPPER_FEE_AMOUNT_SNAPSHOT` | Y | 화주 side 수수료 금액 |
| `DRIVER_FEE_AMOUNT_SNAPSHOT` | Y | 차주 side 수수료 금액 |
| `SHIPPER_PROMO_APPLIED` | Y | 화주 promo 적용 여부 |
| `DRIVER_PROMO_APPLIED` | Y | 차주 promo 적용 여부 |
| `SHIPPER_CHARGE_AMOUNT_SNAPSHOT` | Y | 화주 청구 총액 |
| `DRIVER_PAYOUT_AMOUNT_SNAPSHOT` | Y | 차주 지급 금액 |
| `TOSS_FEE_RATE_SNAPSHOT` | Y | Toss 수수료율 |
| `TOSS_FEE_AMOUNT_SNAPSHOT` | Y | Toss 수수료 금액 |
| `PLATFORM_GROSS_REVENUE_SNAPSHOT` | Y | side fee 총합 |
| `PLATFORM_NET_REVENUE_SNAPSHOT` | Y | toss 차감 후 순수익 |
| `NEGATIVE_MARGIN` | Y | 손익 음수 여부 |
| `POLICY_CONFIG_ID_SNAPSHOT` | Y | 적용 정책 row id |
| `POLICY_UPDATED_AT_SNAPSHOT` | Y | 정책 시각 |
| `PAYMENT_PROVIDER` | Y | `TOSS`, `MANUAL`, etc |

권장 원칙:

- 기존 `AMOUNT`, `FEE_RATE_SNAPSHOT`, `FEE_AMOUNT_SNAPSHOT`, `NET_AMOUNT_SNAPSHOT`는 migration 기간 동안 유지 가능
- 단, 신규 코드에서는 위 canonical bilateral 필드명을 사용

---

## 3. TRANSPORT_PAYMENTS Legacy 호환 규칙

레거시 필드는 아래처럼 backward compatibility 용도로만 유지한다.

| legacy 필드 | snapshot 동기화 규칙 |
| --- | --- |
| `AMOUNT` | `SHIPPER_CHARGE_AMOUNT_SNAPSHOT`와 동일 |
| `FEE_RATE_SNAPSHOT` | `SHIPPER_FEE_RATE_SNAPSHOT`와 동일 |
| `FEE_AMOUNT_SNAPSHOT` | `SHIPPER_FEE_AMOUNT_SNAPSHOT`와 동일 |
| `NET_AMOUNT_SNAPSHOT` | `DRIVER_PAYOUT_AMOUNT_SNAPSHOT`와 동일 |

즉:

- `legacy feeRateSnapshot = shipperFeeRate`
- `legacy feeAmountSnapshot = shipperFeeAmount`
- `legacy netAmountSnapshot = driverPayoutAmount`

---

## 4. SETTLEMENT 권장 필드

`SETTLEMENT`는 canonical store가 아니다. 다만 화면/운영 조회를 위해 아래 subset은 권장한다.

| 필드명 | 필수 | 설명 |
| --- | --- | --- |
| `PAYMENT_ID` | Y | transport payment 연결 |
| `BASE_AMOUNT_SNAPSHOT` | N | 조회 최적화용 |
| `SHIPPER_CHARGE_AMOUNT_SNAPSHOT` | N | 화주 총 결제 금액 |
| `DRIVER_PAYOUT_AMOUNT_SNAPSHOT` | N | 차주 수령 금액 |
| `SHIPPER_FEE_AMOUNT_SNAPSHOT` | N | 화주 side fee |
| `DRIVER_FEE_AMOUNT_SNAPSHOT` | N | 차주 side fee |
| `TOSS_FEE_AMOUNT_SNAPSHOT` | N | toss fee |
| `PLATFORM_NET_REVENUE_SNAPSHOT` | N | 순수익 |
| `NEGATIVE_MARGIN` | N | 손익 음수 여부 |

운영 원칙:

- `SETTLEMENT_RESPONSE`는 최종적으로 bilateral snapshot 필드를 노출할 수 있어야 한다.
- 실제 값은 `TransportPayment` snapshot과 불일치하면 안 된다.

---

## 5. FEE_INVOICE_ITEMS / FEE_INVOICES 필드

화주 청구서는 shipper side 원장이다.

### FEE_INVOICE_ITEMS 필수 추가

| 필드명 | 필수 | 설명 |
| --- | --- | --- |
| `BASE_AMOUNT_SNAPSHOT` | N | 원금 |
| `SHIPPER_APPLIED_LEVEL` | N | 적용 레벨 |
| `SHIPPER_FEE_RATE_SNAPSHOT` | Y | 화주 side 수수료율 |
| `SHIPPER_FEE_AMOUNT_SNAPSHOT` | Y | 화주 side 수수료 금액 |
| `SHIPPER_PROMO_APPLIED` | Y | 화주 promo 적용 여부 |
| `SHIPPER_CHARGE_AMOUNT_SNAPSHOT` | Y | 화주 결제 총액 |
| `POLICY_CONFIG_ID_SNAPSHOT` | N | 정책 추적용 |

### FEE_INVOICES 유지 원칙

- `TOTAL_FEE`는 invoice period의 `SHIPPER_FEE_AMOUNT_SNAPSHOT` 합계
- gross/net revenue를 invoice header에 억지로 넣지 않는다.
- invoice는 화주 billing artifact이지 플랫폼 전체 손익 원장이 아니다.

---

## 6. DRIVER_PAYOUT_ITEMS 필드

차주 지급 원장은 driver side 원장이다.

### DRIVER_PAYOUT_ITEMS 필수 추가

| 필드명 | 필수 | 설명 |
| --- | --- | --- |
| `BASE_AMOUNT_SNAPSHOT` | N | 원금 |
| `DRIVER_APPLIED_LEVEL` | N | 적용 레벨 |
| `DRIVER_FEE_RATE_SNAPSHOT` | Y | 차주 side 수수료율 |
| `DRIVER_FEE_AMOUNT_SNAPSHOT` | Y | 차주 side 수수료 금액 |
| `DRIVER_PROMO_APPLIED` | Y | 차주 promo 적용 여부 |
| `DRIVER_PAYOUT_AMOUNT_SNAPSHOT` | Y | 차주 지급 금액 |
| `POLICY_CONFIG_ID_SNAPSHOT` | N | 정책 추적용 |

기존 `NET_AMOUNT`는 아래처럼 해석한다.

- `NET_AMOUNT = DRIVER_PAYOUT_AMOUNT_SNAPSHOT`

---

## 7. DTO 노출 기준

### TransportPaymentResponse

반드시 최종적으로 아래 필드를 내려야 한다.

- `baseAmount`
- `shipperAppliedLevel`
- `driverAppliedLevel`
- `shipperFeeRate`
- `driverFeeRate`
- `shipperFeeAmount`
- `driverFeeAmount`
- `shipperPromoApplied`
- `driverPromoApplied`
- `shipperChargeAmount`
- `driverPayoutAmount`
- `tossFeeRate`
- `tossFeeAmount`
- `platformGrossRevenue`
- `platformNetRevenue`
- `negativeMargin`
- `policyConfigId`
- `policyUpdatedAt`

### SettlementResponse

사용자/관리자 화면 분리를 고려해 최소 아래 fields를 지원한다.

- `paymentAmount`
  - 의미: `shipperChargeAmount`

- `paymentFeeAmount`
  - 현재는 `shipperFeeAmount` 의미로 해석

- `paymentNetAmount`
  - 현재는 `driverPayoutAmount` 의미로 해석

추가 권장 bilateral fields:

- `shipperFeeRate`
- `driverFeeRate`
- `shipperFeeAmount`
- `driverFeeAmount`
- `shipperPromoApplied`
- `driverPromoApplied`
- `shipperChargeAmount`
- `driverPayoutAmount`
- `tossFeeAmount`
- `platformNetRevenue`
- `negativeMargin`

---

## 8. 생성/동기화 시점

### mark paid 시점

생성/동기화:

- `TransportPayment` canonical snapshot 전체
- `Settlement` 운영용 subset

### confirm / payout item 생성 시점

생성/동기화:

- `DriverPayoutItem` driver-side subset

### fee invoice batch 시점

생성/동기화:

- `FeeInvoiceItem` shipper-side subset
- `FeeInvoice.totalFee`

원칙:

- downstream 원장은 원본 snapshot을 다시 계산하지 않는다.
- 원본은 `TransportPayment`다.

---

## 9. 필수 인덱스/조회 메모

### TRANSPORT_PAYMENTS

- `ORDER_ID` unique 유지
- `SHIPPER_USER_ID`
- `DRIVER_USER_ID`
- `STATUS`
- `NEGATIVE_MARGIN`
- `POLICY_CONFIG_ID_SNAPSHOT`

### DRIVER_PAYOUT_ITEMS

- `ORDER_ID` unique 유지
- `DRIVER_USER_ID`
- `STATUS`

### FEE_INVOICE_ITEMS

- `INVOICE_ID`
- `ORDER_ID`

---

## 10. 이번 문서의 결론

1. 전체 bilateral snapshot은 `TRANSPORT_PAYMENTS`에 고정 저장한다.
2. `SETTLEMENT`, `FEE_INVOICE_ITEMS`, `DRIVER_PAYOUT_ITEMS`는 자기 목적에 필요한 subset만 복제한다.
3. 기존 단면 필드는 migration 동안만 호환 용도로 남기고, 새 코드에서는 bilateral canonical 필드를 사용한다.
