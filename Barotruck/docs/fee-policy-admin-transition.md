# 양면 수수료 정책 관리자 API 전환 메모

## 핵심 계약

- `GET /api/admin/payment/fee-policy`와 `GET /api/admin/payment/fee-policy/current`는 이제 `shipperSide`, `driverSide`, `shipperFirstPaymentPromoRate`, `driverFirstTransportPromoRate`, `tossRate`를 함께 응답한다.
- 기존 평탄 필드 `level0Rate`, `level1Rate`, `level2Rate`, `level3PlusRate`, `firstPaymentPromoRate`는 유지된다.
- 평탄 필드는 전부 `shipperSide`의 alias다. 기존 관리자 화면과 기존 소비자는 그대로 읽을 수 있다.

## PATCH 요청 호환성

- 신규 권장 형식은 아래와 같다.

```json
{
  "shipperSide": {
    "level0Rate": 0.025,
    "level1Rate": 0.02,
    "level2Rate": 0.018,
    "level3PlusRate": 0.015
  },
  "driverSide": {
    "level0Rate": 0.025,
    "level1Rate": 0.02,
    "level2Rate": 0.018,
    "level3PlusRate": 0.015
  },
  "shipperFirstPaymentPromoRate": 0.015,
  "driverFirstTransportPromoRate": 0.015,
  "tossRate": 0.10,
  "minFee": 2000
}
```

- 레거시 요청도 계속 허용한다.
- 레거시 평탄 필드를 보내면 `shipperSide`만 갱신된다.
- `driverSide`, `driverFirstTransportPromoRate`, `tossRate`를 보내지 않으면 현재 저장값을 유지한다.

## 검증 규칙

- 모든 rate 필드는 `0.0000` 이상 `1.0000` 이하여야 한다.
- `minFee`는 `0` 이상이어야 한다.
- `POST /api/admin/payment/fee-policy/levels`의 `side`는 `shipper` 또는 `driver`만 허용한다.
- 빈 PATCH 본문처럼 갱신 대상이 하나도 없으면 거부한다.

## 배포 순서

1. `scripts/db/oracle/add_dual_fee_policy_columns.sql`을 먼저 실행한다.
2. 백엔드를 배포한다.
3. 관리자 프론트를 신규 `shipperSide/driverSide` 응답 사용 방식으로 올린다.

## 기본값

- shipper side: `2.5% / 2.0% / 1.8% / 1.5%`
- driver side: `2.5% / 2.0% / 1.8% / 1.5%`
- shipper first payment promo: `1.5%`
- driver first transport promo: `1.5%`
- toss rate: `10.0%`
