# Payment Postman 테스트 케이스

## 환경 변수
- `baseUrl` 예: `http://localhost:8080`
- `shipper_token`
- `driver_token`
- `order_id` 예: `637`
- `pg_order_id` 예: `TOSS-637-1772214437316`
- `payment_key` 예: `tviva202602280247223yRv3`
- `amount` 예: `55000`
- `payout_ref` 예: `PO-12-3F`
- `seller_id` 예: `seller_20260310`
- `toss_webhook_secret` 예: `{{payment.toss.webhook-secret}}`

## 테스트 케이스 표
| ID | 시나리오 | Method / URL | Auth | Body | 기대 결과 |
|---|---|---|---|---|---|
| TC-01 | `toss/confirm` 1차 호출 | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/confirm` | `Bearer {{shipper_token}}` | `{"paymentKey":"{{payment_key}}","pgOrderId":"{{pg_order_id}}","amount":{{amount}}}` | `200`, `success=true` |
| TC-02 | `toss/confirm` 멱등(동일 요청 2차) | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/confirm` | `Bearer {{shipper_token}}` | TC-01 동일 | `200`, `success=true` (동일 결과) |
| TC-03 | `amount` 불일치 실패 | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/confirm` | `Bearer {{shipper_token}}` | `{"paymentKey":"{{payment_key}}","pgOrderId":"{{pg_order_id}}","amount":1}` | `400`, `code=ILLEGAL_STATE`, `message`에 `amount mismatch` |
| TC-04 | 잘못된/만료 `paymentKey` 실패 | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/confirm` | `Bearer {{shipper_token}}` | `{"paymentKey":"test_invalid_{{$timestamp}}","pgOrderId":"{{fresh_pg_order_id}}","amount":{{amount}}}` | `400`, `code=ILLEGAL_STATE`, `message`에 `NOT_FOUND_PAYMENT_SESSION` |
| TC-05 | 웹훅 수신 성공 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"PAYMENT_STATUS_CHANGED","status":"CANCELED","orderId":"{{pg_order_id}}","paymentKey":"{{payment_key}}"}` + 헤더 `Toss-Event-Id: manual-webhook-{{$timestamp}}` | `200`, `success=true`, `data=true` |
| TC-06 | 웹훅 멱등(동일 EventId 재전송) | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | TC-05와 동일 + 동일 `Toss-Event-Id` | 1차/2차 모두 `200`, `success=true` |
| TC-07 | 무인증 접근 차단 | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/prepare` | 없음 | `{"method":"CARD","payChannel":"APP_CARD","orderName":"AuthCheck"}` | `403 Access Denied` |
| TC-08 | 권한 불일치 차단(Driver로 prepare) | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/toss/prepare` | `Bearer {{driver_token}}` | TC-07 동일 | `403 Access Denied` |
| TC-09 | 권한 불일치 차단(Shipper로 driver confirm) | `POST {{baseUrl}}/api/v1/payments/orders/{{order_id}}/confirm` | `Bearer {{shipper_token}}` | 없음 | `403 Access Denied` |
| TC-10 | `seller.changed` 반영 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"seller.changed","data":{"sellerId":"{{seller_id}}","status":"PARTIALLY_APPROVED"}}` + 헤더 `Toss-Event-Id: seller-{{$timestamp}}`, `Toss-Webhook-Secret: {{toss_webhook_secret}}` | `200`, `success=true`, 차주 `tossPayoutSellerStatus=PARTIALLY_APPROVED` |
| TC-11 | `payout.changed` 요청 상태 반영 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"payout.changed","data":{"payoutId":"{{payout_ref}}","status":"REQUESTED","message":"manual test"}}` + 헤더 `Toss-Event-Id: payout-requested-{{$timestamp}}`, `Toss-Webhook-Secret: {{toss_webhook_secret}}` | `200`, `success=true`, `DriverPayoutItem.status=REQUESTED` |
| TC-12 | `payout.changed` 완료 반영 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"payout.changed","data":{"payoutId":"{{payout_ref}}","status":"COMPLETED"}}` + 헤더 `Toss-Event-Id: payout-completed-{{$timestamp}}`, `Toss-Webhook-Secret: {{toss_webhook_secret}}` | `200`, `success=true`, `DriverPayoutItem.status=COMPLETED`, `feeCompleteDate` 반영 |
| TC-13 | 완료 후 stale 실패 웹훅 무시 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"payout.changed","data":{"payoutId":"{{payout_ref}}","status":"FAILED","message":"stale failure"}}` + 헤더 `Toss-Event-Id: payout-failed-{{$timestamp}}`, `Toss-Webhook-Secret: {{toss_webhook_secret}}` | `200`, `success=true`, 기존 `DriverPayoutItem.status=COMPLETED` 유지 |
| TC-14 | `CANCEL_STATUS_CHANGED`가 `WAIT` 정산을 덮지 않음 | `POST {{baseUrl}}/api/v1/payments/webhooks/toss` | 없음 | `{"eventType":"CANCEL_STATUS_CHANGED","orderId":"{{pg_order_id}}","paymentKey":"{{payment_key}}","status":"CANCELED","cancels":[{"cancelReason":"manual cancel","cancelAmount":{{amount}},"canceledAt":"2026-03-10T10:00:00+09:00","transactionKey":"cancel_tx_1"}]}` + 헤더 `Toss-Event-Id: cancel-wait-{{$timestamp}}`, `Toss-Webhook-Secret: {{toss_webhook_secret}}` | `200`, `success=true`, 기존 `Settlement.status=WAIT` 유지 |

## Postman Tests 스니펫

### 공통 성공(200 + success=true)
```javascript
pm.test("status 200", function () {
  pm.response.to.have.status(200);
});
pm.test("success true", function () {
  const json = pm.response.json();
  pm.expect(json.success).to.eql(true);
});
```

### 공통 실패(400 + ILLEGAL_STATE)
```javascript
pm.test("status 400", function () {
  pm.response.to.have.status(400);
});
pm.test("code is ILLEGAL_STATE", function () {
  const json = pm.response.json();
  pm.expect(json.code).to.eql("ILLEGAL_STATE");
});
```

### 공통 권한 실패(403)
```javascript
pm.test("status 403", function () {
  pm.response.to.have.status(403);
});
```

## 주의 사항
- TC-04는 반드시 **새 prepare 직후** 실행해야 합니다. 이미 `CONFIRMED`인 거래에선 idempotent로 성공 응답이 나올 수 있습니다.
- `fresh_pg_order_id`는 TC-04 전에 새로 호출한 `prepare` 응답값을 사용해야 합니다.

## Payout Webhook 수동 점검 순서
1. 차주 지급 대상 주문 1건을 준비해 `DriverPayoutItem`과 `payout_ref`를 확인합니다.
2. TC-11로 `REQUESTED` 이벤트를 보내고 아이템이 그대로 `REQUESTED`인지 확인합니다.
3. TC-12로 `COMPLETED` 이벤트를 보내고 아이템이 `COMPLETED`로 바뀌며 `SETTLEMENT.FEE_COMPLETE_DATE`가 채워지는지 확인합니다.
4. 정산이 이미 `WAIT`인 주문이면 TC-12 이후에도 `SETTLEMENT.STATUS=WAIT`가 유지되는지 확인합니다.
5. 같은 `payout_ref`로 TC-13을 보내서 stale 실패 이벤트가 와도 `DriverPayoutItem.status=COMPLETED`가 유지되는지 확인합니다.
6. 같은 `Toss-Event-Id`로 TC-10~TC-13 중 하나를 재전송해 웹훅 이벤트 원장이 중복 생성되지 않는지 확인합니다.
