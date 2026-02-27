# Payment Postman 테스트 케이스

## 환경 변수
- `baseUrl` 예: `http://localhost:8080`
- `shipper_token`
- `driver_token`
- `order_id` 예: `637`
- `pg_order_id` 예: `TOSS-637-1772214437316`
- `payment_key` 예: `tviva202602280247223yRv3`
- `amount` 예: `55000`

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
