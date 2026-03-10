# Payment Test App Runbook

기준일: 2026-03-10

이 문서는 테스트 앱으로 `결제 -> 정산 -> 지급 -> 차주 수령 확인`을 재현할 때 필요한 단계별 기대 결과, raw payload 기록 형식, 실패 시 진단 순서를 정리한 런북이다.

상위 기준 문서:

1. `docs/toss-payment-test-app-plan.md`
2. `docs/payment-postman-test-cases.md`
3. `docs/toss-payment-gap-review.md`
4. `docs/payment-logic.md`

---

## 1. 이 런북이 다루는 기본 시나리오

앱 섹션 기준 기본 흐름:

1. `환경 점검`
2. `계정 전환`
3. `시나리오 준비`
4. `화주 결제`
5. `차주 확인`
6. `관리자 지급`
7. `차주 수령 확인`
8. `raw payload / 타임라인`

기본 actor:

1. SHIPPER
2. DRIVER
3. ADMIN

기본 성공 시나리오:

1. SHIPPER가 주문에 대해 결제를 반영한다.
2. DRIVER가 결제를 확인한다.
3. ADMIN이 payout을 요청하고 상태를 동기화한다.
4. DRIVER가 본인 지급 상태를 최종 확인한다.

---

## 2. 사전 조건

시작 전 아래가 충족돼야 한다.

| 항목 | 필요 조건 | 비고 |
| --- | --- | --- |
| 서버 | 테스트 앱이 접근 가능한 서버 URL | local/dev/staging 중 하나 |
| 주문 | 운송 완료 이후 상태의 주문 1건 | `orderId` 확보 필요 |
| SHIPPER | 결제 가능한 본인 주문 보유 | 토큰 또는 앱 로그인 가능 |
| DRIVER | 해당 주문의 배정 기사 | payout 상태 확인 권한 필요 |
| ADMIN | payout request/sync 가능 계정 | `/api/admin/payment/**` 접근 |
| Toss 결제 | `REAL_TOSS_PAYMENT_E2E`면 test key 준비 | 미준비면 manual path 사용 |
| Toss payout | payout API 키 또는 mock 정책 확인 | 결과 기록 시 mock 여부 명시 |

---

## 3. 단계별 기대 결과 표

### 3.1 메인 성공 시나리오

| Step | Actor | 앱 액션 | 기준 API | 기대 결과 | 저장할 식별자 |
| --- | --- | --- | --- | --- | --- |
| S1 | SHIPPER | billing context 조회 | `GET /api/v1/payments/billing/context` | `clientKey`, `customerKey`, redirect URL 확인 | `customerKey` |
| S2 | SHIPPER | billing agreement 조회 | `GET /api/v1/payments/billing/agreements/me` | 최신 agreement 상태 확인 | `agreementId`, `status` |
| S3 | SHIPPER | 필요 시 billing 등록 | `POST /api/v1/payments/billing/agreements` | 신규 또는 갱신 agreement 저장 | `agreementId` |
| S4A | SHIPPER | Toss prepare | `POST /api/v1/payments/orders/{orderId}/toss/prepare` | `pgOrderId`, `amount`, redirect URL 생성 | `pgOrderId`, `amount` |
| S4B | SHIPPER | Toss confirm | `POST /api/v1/payments/orders/{orderId}/toss/confirm` | gateway tx `CONFIRMED`, 내부 결제 `PAID` | `paymentKey`, tx status |
| S4C | SHIPPER | 수동 mark-paid | `POST /api/v1/payments/orders/{orderId}/mark-paid` | 내부 결제 `PAID`, 정산 `READY` | proofUrl 또는 수동 메모 |
| S5 | DRIVER | driver confirm | `POST /api/v1/payments/orders/{orderId}/confirm` | 내부 결제 `CONFIRMED`, 주문 `CONFIRMED`, 정산 `COMPLETED` | confirm 시각 |
| S6 | ADMIN | payout request | `POST /api/admin/payment/orders/{orderId}/payouts/request` | payout item 생성, `REQUESTED` 또는 `COMPLETED` | `itemId`, `payoutRef` |
| S7 | ADMIN | payout status 조회 | `GET /api/admin/payment/payout-items/orders/{orderId}/status` | seller/webhook/payout 상태 확인 | sellerId, sellerStatus |
| S8 | ADMIN | payout sync | `POST /api/admin/payment/payout-items/orders/{orderId}/sync` | polling 후 최신 payout 상태 반영 | sync 시각 |
| S9 | DRIVER | payout status 조회 | `GET /api/v1/payments/payouts/orders/{orderId}/status` | 차주 본인 기준 지급 상태 확인 | driver-visible status |
| S10 | SHIPPER | 필요 시 billing cleanup | `DELETE /api/v1/payments/billing/agreements/me` | 임시 등록 카드 상태 정리 | cleanup 여부 |

실행 분기:

1. 실결제 검증이면 `S4A`, `S4B` 수행
2. 정산/지급 파이프라인만 검증이면 `S4C` 수행

### 3.2 상태 기대값 매트릭스

| 시점 | PaymentGatewayTransaction | TransportPayment | Order | Settlement | DriverPayoutItem |
| --- | --- | --- | --- | --- | --- |
| 결제 전 | 없음 또는 `PREPARED` 이전 상태 | `READY` 또는 없음 | 운송 완료 이후 상태 | 초기 상태 | 없음 |
| Toss prepare 직후 | `PREPARED` | 변화 없음 | 변화 없음 | 변화 없음 | 없음 |
| Toss confirm 성공 직후 | `CONFIRMED` | `PAID` | `PAID` | `READY` | 없음 |
| 수동 mark-paid 직후 | 실gateway 없음 또는 기존 값 유지 | `PAID` | `PAID` | `READY` | 없음 |
| driver confirm 직후 | 변화 없음 | `CONFIRMED` | `CONFIRMED` | `COMPLETED` | 없음 |
| payout request 직후 | 변화 없음 | 변화 없음 | 변화 없음 | `COMPLETED` 유지 | `REQUESTED` 또는 `COMPLETED` |
| payout sync/webhook 반영 후 | 변화 없음 | 변화 없음 | 변화 없음 | `COMPLETED` 유지 | `COMPLETED` 또는 최종 상태 |
| driver 수령 확인 시 | 변화 없음 | 변화 없음 | 변화 없음 | `COMPLETED` 유지 | 차주 화면과 동일 상태 |
| billing cleanup 후 | 변화 없음 | 변화 없음 | 변화 없음 | 변화 없음 | agreement 상태만 `INACTIVE` 가능 |

주의:

1. cancel 시나리오는 본 런북의 주경로가 아니다.
2. payout item이 이미 있으면 결제 cancel은 금지된다.
3. 실환경과 mock 환경은 결과를 같은 표에 섞지 않는다.

---

## 4. Raw Payload 기록 형식

### 4.1 최소 기록 단위

각 단계에서 아래 필드를 남긴다.

| 필드 | 설명 |
| --- | --- |
| `capturedAt` | 캡처 시각 |
| `stepId` | `S1` 같은 단계 ID |
| `actor` | `SHIPPER`, `DRIVER`, `ADMIN`, `SYSTEM` |
| `appSection` | 앱 화면 섹션명 |
| `endpoint` | 실제 호출 API |
| `method` | `GET`, `POST`, `DELETE` |
| `requestSummary` | 핵심 요청값 요약 |
| `responseStatus` | HTTP status |
| `success` | 응답 success 여부 |
| `orderId` | 공통 주문 ID |
| `pgOrderId` | Toss 준비 거래 ID |
| `paymentKey` | Toss 결제 식별자 |
| `payoutRef` | 지급 참조값 |
| `rawPayload` | 원문 또는 원문 저장 위치 |
| `notes` | UI 메시지, 실패 사유, 특이사항 |

### 4.2 권장 JSON 템플릿

```json
{
  "capturedAt": "2026-03-10T14:30:00+09:00",
  "stepId": "S4B",
  "actor": "SHIPPER",
  "appSection": "화주 결제",
  "endpoint": "/api/v1/payments/orders/{orderId}/toss/confirm",
  "method": "POST",
  "requestSummary": {
    "orderId": 123,
    "pgOrderId": "TOSS-123-1772214437316",
    "amount": 55000
  },
  "responseStatus": 200,
  "success": true,
  "orderId": 123,
  "pgOrderId": "TOSS-123-1772214437316",
  "paymentKey": "tviva202602280247223yRv3",
  "payoutRef": null,
  "rawPayload": "{...}",
  "notes": "confirm success"
}
```

### 4.3 파일명 규칙

raw payload를 파일로 저장한다면 아래 규칙을 권장한다.

`{runDate}_{runType}_{orderId}_{stepId}.json`

예시:

- `20260310_REAL_TOSS_637_S4A.json`
- `20260310_REAL_TOSS_637_S4B.json`
- `20260310_REAL_TOSS_637_S8.json`

---

## 5. 실패 시 진단 순서

실패하면 아래 순서대로 본다.

### 5.1 1차: 환경 문제 확인

1. 테스트 앱 `환경 점검`에서 서버 URL이 맞는지 확인
2. Toss 결제 키와 payout 키가 준비 상태인지 확인
3. mock 여부를 결과 시트에 기록했는지 확인
4. actor가 SHIPPER/DRIVER/ADMIN 중 올바르게 전환됐는지 확인

대표 증상:

- prepare 버튼 비활성
- payout request 버튼 미노출
- 로그인은 됐지만 API가 전부 401/403

### 5.2 2차: 주문 전제조건 확인

1. 주문이 운송 완료 이후 상태인지 확인
2. 이미 payout item이 존재하는 주문인지 확인
3. 같은 주문으로 직전 실패 테스트가 남아 있는지 확인
4. 차주 계좌/셀러 상태가 payout 가능한지 확인

대표 증상:

- 결제 시작 불가
- driver confirm 불가
- payout request 직후 실패

### 5.3 3차: 결제 단계 진단

1. `billing context`가 비어 있는지 확인
2. `billing agreement me`가 `ACTIVE`인지 확인
3. Toss prepare 응답에서 `pgOrderId`, `amount`가 정상인지 확인
4. Toss confirm 또는 mark-paid 이후 `TransportPayment=PAID`, `Settlement=READY`가 됐는지 확인
5. raw payload 패널에서 confirm/mark-paid 원문을 확보했는지 확인

대표 증상:

- prepare는 성공했는데 결제창 진입 실패
- 결제는 성공했는데 내부 상태가 `PAID`로 안 바뀜
- `paymentKey` 누락

### 5.4 4차: 차주 확인 단계 진단

1. DRIVER actor가 해당 주문의 실제 기사인지 확인
2. `TransportPayment`가 이미 `PAID`인지 확인
3. confirm 이후 `Order=CONFIRMED`, `Settlement=COMPLETED`로 바뀌는지 확인

대표 증상:

- 차주 confirm 403
- confirm 호출 후 상태 변화 없음

### 5.5 5차: 지급 단계 진단

1. `payout request` 직후 `itemId`, `payoutRef`가 생기는지 확인
2. `payout item status`에서 `failureReason`, `sellerStatus`, `latest webhook` 필드를 확인
3. `payout sync` 이후 상태가 `REQUESTED/COMPLETED/FAILED` 중 하나로 수렴하는지 확인
4. payout 상태와 webhook 요약이 서로 충돌하는지 확인

대표 증상:

- payout item 미생성
- `FAILED`만 반복
- `sellerStatus` 미승인
- sync 이후에도 상태 미변경

### 5.6 6차: 차주 수령 확인 진단

1. DRIVER actor로 `/payouts/orders/{orderId}/status` 조회가 되는지 확인
2. ADMIN 화면의 payout status와 DRIVER 화면의 payout status가 같은지 확인
3. 차주 본인 주문 접근인지 확인

대표 증상:

- driver status 403
- admin은 `COMPLETED`인데 driver는 조회 실패

---

## 6. 실패 결과 요약 형식

실패했으면 아래 형식으로 남긴다.

```text
[QA FAILURE]
run_id:
failed_step:
failed_actor:
order_id:
run_type:
symptom:
api_or_screen:
last_success_step:
captured_ids:
  - agreementId:
  - pgOrderId:
  - paymentKey:
  - payoutRef:
root_cause_candidate:
next_action:
raw_payload_location:
```

---

## 7. 최종 완료 정의

아래가 모두 충족되면 해당 실행을 `재현 가능`으로 본다.

1. 실행 타입과 actor 전환 순서가 기록돼 있다.
2. 주문 ID, `pgOrderId`, `paymentKey`, `payoutRef` 중 해당 경로에 필요한 식별자가 남아 있다.
3. 각 단계의 기대 상태와 실제 상태를 비교한 표가 남아 있다.
4. raw payload 또는 타임라인 로그가 단계별로 남아 있다.
5. 실패했다면 어느 단계에서 막혔는지와 다음 진단 순서가 남아 있다.
