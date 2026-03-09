# Notification Points

이 문서는 현재 코드 기준으로 "알림이 이미 발송되는 부분"과 "추가가 필요한 부분"을 정리한 문서입니다.

기준 전제:

- `driverNo == 차주 userId`
- `NotificationService.sendNotification(Users user, ...)`
- `NotificationService.sendNotification(Long driverNo, ...)`

즉 숫자 하나만 있을 때는 `driverNo/userId`를 그대로 넘겨도 됩니다.

## 1. 현재 이미 발송되는 알림

### 1-1. 채팅 메시지 수신

- 위치: `domain/chat/service/ChatService.java`
- 메서드: `sendMessage(...)`
- 수신자: 채팅방의 나를 제외한 모든 멤버
- 타입: `CHAT`
- 내용:
  - 텍스트면 메시지 내용
  - 이미지면 `"사진을 보냈습니다."`

### 1-2. 즉시배차 오더 수락

- 위치: `domain/order/service/OrderService.java`
- 메서드: `acceptOrder(...)`
- 조건: `order.getSnapshot().isInstant() == true`
- 수신자: 화주 `order.getUser()`
- 타입: `ORDER`
- 제목: `배차 완료`
- 내용: 기사 배차 완료 + 차량번호

### 1-3. 운송 상태 변경

- 위치: `domain/order/service/OrderService.java`
- 메서드: `updateStatus(...)`
- 수신자: 화주 `order.getUser()`
- 타입: `ORDER`
- 제목: `주문 상태 변경`
- 내용: 상태가 `상차/이동중/하차/완료` 등으로 변경되었음을 안내

## 2. 우선 추가가 필요한 알림

아래는 현재 비즈니스 흐름상 알림이 있으면 맞는데, 아직 `NotificationService` 호출이 없는 부분입니다.

### 2-1. 일반 오더 지원 접수

- 위치: `domain/order/service/OrderService.java`
- 메서드: `acceptOrder(...)`
- 조건: 즉시배차가 아닌 경우 (`driverList`에만 추가되는 분기)
- 수신자: 화주
- 이유: 지원자가 생겼다는 걸 화주가 바로 알아야 함
- 권장 메시지:
  - 타입: `ORDER`
  - 제목: `배차 지원 도착`
  - 내용: `새로운 기사님이 오더에 지원했습니다.`

### 2-2. 화주의 최종 차주 선택

- 위치: `domain/order/service/OrderService.java`
- 메서드: `selectDriver(...)`
- 수신자:
  - 선택된 차주
  - 필요하면 화주 본인 확인용은 생략 가능
- 이유: 차주가 최종 배차 확정을 즉시 알아야 함
- 권장 메시지:
  - 타입: `ORDER`
  - 제목: `배차 확정`
  - 내용: `화주가 회원님을 최종 기사로 선택했습니다.`

### 2-3. 주문 취소

- 위치: `domain/order/service/OrderService.java`
- 메서드: `cancelOrder(...)`
- 수신자:
  - 화주가 취소하면 차주
  - 차주가 취소하면 화주
  - 신청 철회(`APPLICANT`)는 상대방 알림 선택 사항
- 이유: 취소는 상대방에게 반드시 전달돼야 함
- 권장 메시지:
  - 타입: `ORDER`
  - 제목: `주문 취소`
  - 내용: `상대방에 의해 주문이 취소되었습니다.`

### 2-4. 관리자 강제배차

- 위치: `domain/order/service/AdminOrderService.java`
- 메서드: `forceAllocateOrder(...)`
- 수신자:
  - 차주
  - 화주
- 이유: 관리자 개입 배차는 일반 배차와 구분해서 알려야 함
- 권장 메시지:
  - 타입: `ORDER`
  - 제목: `관리자 강제배차`
  - 내용: `관리자에 의해 배차가 확정되었습니다.`

### 2-5. 관리자 강제취소

- 위치: `domain/order/service/AdminOrderService.java`
- 메서드: `adminCancelOrder(...)`
- 수신자:
  - 화주
  - 배정된 차주가 있으면 차주
- 이유: 관리자 취소는 일반 취소와 별도 안내가 필요

## 3. 결제/정산 알림에서 우선 필요한 부분

### 3-1. 화주 결제 완료

- 위치: `domain/payment/service/core/PaymentLifecycleService.java`
- 메서드: `markPaid(...)`
- 수신자: 배정된 차주
- 이유: 차주가 최종 확인 가능한 상태(`PAID`)가 되었음을 알아야 함
- 권장 메시지:
  - 타입: `PAYMENT`
  - 제목: `화주 결제 완료`
  - 내용: `화주가 결제를 완료했습니다. 최종 확인을 진행해주세요.`

### 3-2. 토스 결제 확정

- 위치: `domain/payment/service/core/PaymentLifecycleService.java`
- 메서드: `applyPaidFromGatewayTx(...)`
- 수신자: 배정된 차주
- 이유: 토스 결제도 결국 차주 확인 전 단계에서는 동일하게 알려야 함

### 3-3. 차주 최종 수락

- 위치: `domain/payment/service/core/PaymentLifecycleService.java`
- 메서드: `confirmByDriver(...)`
- 수신자: 화주
- 이유: 결제가 최종 확정되었음을 화주가 알아야 함
- 권장 메시지:
  - 타입: `PAYMENT`
  - 제목: `정산 확인 완료`
  - 내용: `차주가 결제를 최종 확인했습니다.`

### 3-4. 이의제기 등록

- 위치: `domain/payment/service/core/PaymentDisputeService.java`
- 등록 메서드: 분쟁 생성 경로
- 수신자:
  - 상대 당사자
  - 관리자(운영 알림이 있으면)
- 이유: 정산 보류 상태 진입을 즉시 알려야 함

### 3-5. 이의제기 처리 결과

- 위치: `domain/payment/service/core/PaymentDisputeService.java`
- 메서드: `updateDisputeStatus(...)`
- 수신자:
  - 화주
  - 차주
- 이유: `ADMIN_HOLD`, `ADMIN_FORCE_CONFIRMED`, `ADMIN_REJECTED` 결과를 양측이 알아야 함

## 4. 후순위지만 알림 후보인 부분

### 4-1. 리뷰 등록

- 위치: `domain/review/service/ReviewService.java`
- 메서드: `createReview(...)`
- 수신자: 리뷰 대상자
- 이유: 리뷰가 등록됐음을 알려줄 수 있음

### 4-2. 신고 등록/처리

- 위치: `domain/review/service/ReportService.java`
- 메서드:
  - `createReport(...)`
  - `updateReportStatus(...)`
- 수신자:
  - 관리자
  - 신고자
- 이유: 운영성 이벤트로 의미 있음

### 4-3. 공지 등록

- 위치: `domain/notice/service/NoticeService.java`
- 메서드: `createNotice(...)`
- 수신자: 전체 사용자 또는 역할별 사용자
- 이유: 정책/점검/이벤트 공지 푸시 가능

## 5. 현재 우선순위 제안

실제 체감이 큰 순서대로 붙이면 아래 순서가 맞습니다.

1. `selectDriver(...)` -> 선택된 차주 알림
2. `markPaid(...)` / `applyPaidFromGatewayTx(...)` -> 차주 결제 확인 요청 알림
3. `confirmByDriver(...)` -> 화주 최종 확인 완료 알림
4. `cancelOrder(...)` / `adminCancelOrder(...)` -> 상대방 취소 알림
5. `updateDisputeStatus(...)` -> 분쟁 처리 결과 알림
6. `acceptOrder(...)` 일반 지원 접수 알림

## 6. 구현 원칙

- 오더/결제 본 로직은 유지하고, 알림은 `NotificationService`를 `try/catch`로 감싸서 부수효과로 붙입니다.
- `REQUIRES_NEW`가 이미 들어가 있으므로 알림 실패로 본 트랜잭션이 같이 롤백되지 않게 유지합니다.
- 차주 대상으로 보낼 때는 `driverNo`를 그대로 넘겨도 됩니다.

예시:

```java
notificationService.sendNotification(driverNo, "ORDER", "배차 확정", "화주가 회원님을 선택했습니다.", order.getOrderId());
```
