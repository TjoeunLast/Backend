# Admin Payment Status APIs

관리자 페이지에서 결제/정산 운영 상태를 조회하기 위해 추가한 상태 조회 API 정리 문서입니다.

기준 컨트롤러:
- `src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java`

기준 조회 서비스:
- `src/main/java/com/example/project/domain/payment/service/query/AdminPaymentStatusQueryService.java`

베이스 경로:
- `/api/admin/payment`

인증:
- `/api/admin/**`
- `ADMIN` 권한 필요

## 추가된 상태 조회 API

### 1. 주문 이의제기 상태 조회

- `GET /api/admin/payment/orders/{orderId}/disputes/status`
- 설명:
  - 특정 주문의 결제 이의제기 현재 상태 조회
- 주요 응답 필드:
  - `disputeId`
  - `orderId`
  - `paymentId`
  - `status`
  - `requestedAt`
  - `processedAt`
- 관리자 페이지 사용처:
  - 결제 이의제기 상세 모달
  - 주문별 이의제기 처리 화면
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/orders/{orderId}/disputes`
  - `PATCH /api/admin/payment/orders/{orderId}/disputes/{disputeId}/status`

### 2. 수수료 청구서 상태 조회

- `GET /api/admin/payment/fee-invoices/status?shipperUserId={id}&period=YYYY-MM`
- 설명:
  - 특정 화주의 특정 월 수수료 청구 상태 조회
- 주요 응답 필드:
  - `invoiceId`
  - `shipperUserId`
  - `period`
  - `status`
  - `totalFee`
  - `issuedAt`
  - `dueAt`
  - `paidAt`
- 관리자 페이지 사용처:
  - 화주 월별 수수료 청구서 조회 카드
  - 청구서 생성 버튼 옆 상태 표시
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/fee-invoices/run`
  - `POST /api/admin/payment/fee-invoices/generate`

### 3. 차주 지급 배치 상태 조회

- `GET /api/admin/payment/payouts/status?date=YYYY-MM-DD`
- 설명:
  - 특정 날짜 지급 배치 상태 조회
- 주요 응답 필드:
  - `batchId`
  - `batchDate`
  - `status`
  - `totalItems`
  - `failedItems`
  - `requestedAt`
  - `completedAt`
  - `failureReason`
- 관리자 페이지 사용처:
  - 지급 배치 대시보드
  - 일자별 지급 실행 화면
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/payouts/run`

### 4. 주문별 지급 아이템 상태 조회

- `GET /api/admin/payment/payout-items/orders/{orderId}/status`
- 설명:
  - 특정 주문의 차주 지급 아이템 상태 조회
- 주요 응답 필드:
  - `itemId`
  - `orderId`
  - `batchId`
  - `driverUserId`
  - `status`
  - `retryCount`
  - `requestedAt`
  - `completedAt`
  - `failureReason`
  - `payoutRef`
- 관리자 페이지 사용처:
  - 주문 단위 지급 재시도 버튼 영역
  - 지급 실패 상세 팝업
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/payout-items/{itemId}/retry`

### 5. 만료 예정 PREPARED 토스 거래 큐 상태 조회

- `GET /api/admin/payment/toss/expire-prepared/status`
- 설명:
  - 만료 처리 대상 `PREPARED` 토스 거래 큐 상태 조회
- 주요 응답 필드:
  - `status`
  - `candidateCount`
  - `firstTargetAt`
- 관리자 페이지 사용처:
  - PG 운영 화면의 만료 처리 카드
  - 만료 처리 실행 버튼 위 상태 표시
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/toss/expire-prepared/run`

### 6. 토스 실패 재시도 큐 상태 조회

- `GET /api/admin/payment/toss/retries/status`
- 설명:
  - 토스 실패 재시도 큐 상태 조회
- 주요 응답 필드:
  - `status`
  - `candidateCount`
  - `firstTargetAt`
  - `maxRetryAttempts`
- 관리자 페이지 사용처:
  - PG 재시도 운영 화면
  - retry queue 모니터링 카드
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/toss/retries/run`

### 7. 주문별 최신 토스 거래 상태 조회

- `GET /api/admin/payment/toss/orders/{orderId}/status`
- 설명:
  - 특정 주문의 최신 토스 거래 상태 조회
- 주요 응답 필드:
  - `txId`
  - `orderId`
  - `provider`
  - `status`
  - `amount`
  - `retryCount`
  - `expiresAt`
  - `approvedAt`
  - `nextRetryAt`
  - `failCode`
  - `failMessage`
- 관리자 페이지 사용처:
  - 주문 상세의 PG 상태 영역
  - 토스 결제 실패 원인 확인 모달

### 8. 대사 상태 요약 조회

- `GET /api/admin/payment/reconciliation/status`
- 설명:
  - 현재 대사 기준 상태 요약 조회
- 주요 응답 필드:
  - `confirmedGatewayCount`
  - `matchedPaymentCount`
  - `unresolvedMismatchCount`
- 관리자 페이지 사용처:
  - 정산 대사 대시보드
  - 대사 실행 버튼 전 요약 표시
- 같이 쓰는 액션 API:
  - `POST /api/admin/payment/reconciliation/run`

## 관리자 페이지 권장 사용 방식

- 액션 버튼 누르기 전 상태 GET 호출
- 현재 상태를 먼저 표시
- 완료 상태면 실행 버튼 비활성화
- 실패 상태면 재시도/수정 버튼 노출
- 데이터가 없으면 현재 구현상 `400` 응답이 오므로 UI에서는 `데이터 없음`으로 처리

## 관리자 메뉴별 연결 예시

- 결제 이의제기 관리
  - `GET /orders/{orderId}/disputes/status`
- 수수료 청구 관리
  - `GET /fee-invoices/status`
- 차주 지급 관리
  - `GET /payouts/status`
  - `GET /payout-items/orders/{orderId}/status`
- PG 운영
  - `GET /toss/expire-prepared/status`
  - `GET /toss/retries/status`
  - `GET /toss/orders/{orderId}/status`
- 대사/모니터링
  - `GET /reconciliation/status`
