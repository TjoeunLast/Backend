# 프로모션 eligibility 및 레벨 연동 기준

기준일: 2026-03-11

## 목적

- 화주 `첫 결제 프로모션`과 차주 `첫 운송 프로모션`의 적용 여부를 같은 규칙으로 재현한다.
- 취소, 분쟁, 관리자 강제처리, 재시도, 동시성 상황에서도 중복 지급을 막는다.
- 프로모션 판정은 계산 엔진과 분리하고, 계산 자체는 기존 `FeePolicyService` 계열에 맡긴다.

## 레벨 기준

- 사용자 레벨 원본은 `USERS.USER_LEVEL`이다.
- `null` 레벨은 `Lv0`로 정규화한다.
- 프로모션 판정 서비스는 `userId`, `userLevel`, `promoEligible`를 같이 반환한다.
- 즉 계산 엔진은 별도 사용자 조회 없이 같은 판정 결과로 레벨과 프로모션 여부를 함께 받을 수 있다.

## 화주 첫 결제 기준

최종 적용 시점:

- `TransportPayment`가 실제 결제 흐름에 들어가 `PAID`, `CONFIRMED`, `ADMIN_FORCE_CONFIRMED`, `DISPUTED`, `ADMIN_HOLD`, `ADMIN_REJECTED` 같은 유료 상태로 넘어가기 직전
- 이 시점에 `TRANSPORT_PAYMENTS.FIRST_PAYMENT_PROMO_APPLIED`를 확정한다.

판정 규칙:

- 같은 화주에 대해 다른 주문에서 `FIRST_PAYMENT_PROMO_APPLIED = true`가 이미 존재하면 현재 주문은 비적용이다.
- 현재 주문의 `FIRST_PAYMENT_PROMO_APPLIED = true`가 이미 켜져 있으면 재시도에도 동일하게 적용으로 본다.
- `READY` 레코드만 있는 상태는 최종 소모로 보지 않는다.

상태 해석:

- `CANCELLED`로 바뀌어도 이미 `FIRST_PAYMENT_PROMO_APPLIED = true`였던 이력은 유지한다.
- 즉 취소나 환불이 생겨도 프로모션을 다시 열어주지 않는다.
- `DISPUTED`, `ADMIN_HOLD`, `ADMIN_REJECTED`도 이미 결제 흐름에 진입한 건으로 보고 프로모션 소모를 유지한다.

이 원칙을 둔 이유:

- 취소 후 재오픈까지 허용하면 같은 화주가 여러 주문에서 프로모션을 번갈아 점유할 수 있다.
- 현재 스키마에서 과거 상태를 완전히 이벤트 소싱하지 않으므로, 소모 여부는 별도 플래그로 남겨야 재현 가능하다.

## 차주 첫 운송 기준

최종 적용 시점:

- 첫 `DriverPayoutItem` 생성 시점
- 즉 `TransportPayment`가 `CONFIRMED` 또는 `ADMIN_FORCE_CONFIRMED`가 된 뒤, 실제 정산 대상이 만들어질 때

판정 규칙:

- 같은 차주에 대해 다른 주문의 `DRIVER_PAYOUT_ITEMS.FIRST_TRANSPORT_PROMO_APPLIED = true`가 이미 존재하면 현재 주문은 비적용이다.
- 현재 지급 아이템에 `FIRST_TRANSPORT_PROMO_APPLIED = true`가 이미 있으면 재처리, 재요청, 재동기화에서도 계속 적용으로 본다.
- 지급 상태가 `READY`, `REQUESTED`, `FAILED`, `RETRYING`, `COMPLETED` 중 무엇이든, 아이템이 한번 생성되어 프로모션이 찍혔다면 소모된 것으로 본다.

상태 해석:

- 지급 실패나 재시도는 프로모션을 되돌리지 않는다.
- 분쟁이나 관리자 강제확정으로 인해 늦게 지급 아이템이 생성되더라도, 실제 `payout item` 생성 시점이 첫 정산 대상 생성 기준이 된다.

주의:

- 차주 프로모션은 현재 계산 엔진에 직접 연결하지 않았다.
- 이번 변경은 `누가 첫 운송 프로모션 대상인지`를 영속 플래그와 서비스로 확정하는 단계다.

## 취소/분쟁/관리자 처리 원칙

- 취소 전 이미 프로모션 플래그가 찍혔다면 그대로 유지한다.
- 분쟁과 관리자 보류는 `프로모션 소모 이후의 상태 변화`로 본다.
- 관리자 강제확정은 일반 확정과 동일하게 프로모션 소모 대상이다.
- 아직 프로모션 플래그가 찍히지 않은 `READY` 상태만 미소모로 본다.

## 멱등성

- 현재 주문/지급아이템에 이미 플래그가 있으면 같은 결과를 그대로 돌려준다.
- 같은 API나 스케줄이 재호출되어도 추가 프로모션을 발급하지 않는다.
- 프로모션 여부는 상태 카운트가 아니라 영속 플래그로 판정한다.

## 동시성 방어

- 프로모션 최종 확정 시 `USERS` row를 `PESSIMISTIC_WRITE`로 잠근다.
- 잠금 안에서 같은 사용자에 대한 기존 `promo applied` 플래그 존재 여부를 확인한다.
- 따라서 같은 화주/차주에 대해 동시 요청이 들어와도 한 건만 `promo applied = true`가 된다.

## 구현 포인트

- 화주: `PromotionEligibilityService.resolveShipperFirstPaymentContext`
- 차주: `PromotionEligibilityService.applyDriverFirstTransportPromotion`
- 락 진입점: `UserPort.lockRequiredUser`
- 영속 플래그:
  - `TRANSPORT_PAYMENTS.FIRST_PAYMENT_PROMO_APPLIED`
  - `DRIVER_PAYOUT_ITEMS.FIRST_TRANSPORT_PROMO_APPLIED`
