<div align="center">

![Barotruck Banner](https://capsule-render.vercel.app/api?type=waving&color=0:0F172A,50:1D4ED8,100:16A34A&height=220&section=header&text=Barotruck%20Backend&fontSize=42&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Spring%20Boot%20Logistics%20Platform%20Backend&descAlignY=58&descSize=16)

# Barotruck Backend

화주, 기사, 관리자 역할을 기준으로 주문, 배차, 결제, 정산, 채팅, 알림 기능을 제공하는 물류 플랫폼 백엔드입니다.

<table>
  <tr>
    <td align="center"><img src="assets/icons/java.svg" alt="Java" width="52" height="52"><br><strong>Java 17</strong></td>
    <td align="center"><img src="assets/icons/springboot.svg" alt="Spring Boot" width="52" height="52"><br><strong>Spring Boot 3.3.5</strong></td>
    <td align="center"><img src="assets/icons/springsecurity.svg" alt="Spring Security" width="52" height="52"><br><strong>Spring Security</strong></td>
    <td align="center"><img src="assets/icons/oracle.svg" alt="Oracle Database" width="52" height="52"><br><strong>Oracle</strong></td>
    <td align="center"><img src="assets/icons/redis.svg" alt="Redis" width="52" height="52"><br><strong>Redis</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="assets/icons/websocket.svg" alt="WebSocket" width="52" height="52"><br><strong>WebSocket</strong></td>
    <td align="center"><img src="assets/icons/awss3.svg" alt="AWS S3" width="52" height="52"><br><strong>AWS S3</strong></td>
    <td align="center"><img src="assets/icons/firebase.svg" alt="Firebase" width="52" height="52"><br><strong>Firebase</strong></td>
    <td align="center"><img src="assets/icons/swagger.svg" alt="Swagger" width="52" height="52"><br><strong>Swagger</strong></td>
    <td align="center"><img src="assets/icons/jwt.svg" alt="JWT" width="52" height="52"><br><strong>JWT</strong></td>
  </tr>
</table>

</div>

## Overview

Barotruck 백엔드는 Spring Boot 기반의 통합 물류 서비스 서버입니다.  
하나의 애플리케이션 안에서 인증, 주문 관리, 자동 배차, 결제/정산, 채팅, 공지, 리뷰, 관리자 정적 페이지 서빙까지 함께 처리합니다.

## Architecture

```text
src/main/java/com/example/project
|-- domain
|   |-- chat
|   |-- dispatch
|   |-- notice
|   |-- notification
|   |-- order
|   |-- payment
|   |-- proof
|   |-- review
|   `-- settlement
|-- global
|-- member
`-- security
```

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- Spring Validation
- Spring WebSocket
- Spring Actuator

### Data

- Oracle Database
- Spring Data JPA
- JDBC
- Querydsl
- Redis

### External Integrations

- JWT Authentication
- AWS S3
- Firebase Admin SDK
- Toss Payments
- Google OAuth / Maps / Places
- Solapi SMS
- Naver Clova OCR

## Features

### Authentication & Member

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/authenticate`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/find-email`
- `POST /api/v1/auth/reset-password`
- `POST /api/auth/google/signin`
- `POST /api/auth/google/register-complete`
- `POST /api/auth/email-request`
- `POST /api/auth/email-verify`
- `POST /api/auth/sms/request`
- `POST /api/auth/sms/verify`
- 사용자 프로필, 비밀번호, FCM 토큰, 프로필 이미지 관리: `/api/user/**`
- 화주/기사 전용 프로필 관리: `/api/v1/shippers/**`, `/api/v1/drivers/**`
- 관리자 사용자 관리: `/api/v1/admin/user/**`

### Order & Transport

- 주문 생성 및 단건 조회: `/api/v1/orders`
- 기사 추천 목록 조회
- 기사 수락, 주문 취소, 상태 변경
- 화주별 주문 조회, 기사 운행 목록 조회, 매출 조회
- 주문 이미지 및 도착 사진 업로드/조회/삭제
- 관리자 주문 운영 API: `/api/v1/admin/orders/**`

### Dispatch

- 자동 배차 수동 실행: `POST /api/v1/dispatch/orders/{orderId}/run`
- 배차 재시도: `POST /api/v1/dispatch/orders/{orderId}/retry`
- 주문별 배차 상태 조회
- 최근 배차 작업 조회
- 관리자 강제 매칭
- 기사 가용 상태 및 위치 갱신
- 기사 제안 목록 조회, 수락, 거절

### Payment & Settlement

- 수수료 미리보기: `POST /api/v1/payments/fee-preview`
- 주문 결제 완료 처리
- 기사 결제 확인
- Toss 결제 준비 / 승인 / 웹훅 수신
- 빌링키 발급 및 해지
- 기사 지급 상태 조회
- 수수료 청구서 조회 및 납부 처리
- 관리자 결제 운영 API: `/api/admin/payment/**`
- 정산 조회 및 상태 변경: `/api/v1/settlements/**`

### Chat, Notification & Support

- 채팅방 / 메시지 조회: `/api/chat/**`
- WebSocket STOMP endpoint: `/ws-stomp`
- Publish prefix: `/pub/**`
- Subscribe prefix: `/sub/**`
- 알림 조회 및 읽음 처리: `/api/notifications`
- 공지: `/api/notices`, `/api/admin/notices`
- 리뷰 / 신고: `/api/reviews`, `/api/reports`
- OCR API: `/api/ocr/**`
- 동네 검색 API: `/api/neighborhoods/**`
- 운송 증빙 API: `/api/proof/**`

## Admin Pages

정적 관리자 화면은 `src/main/resources/static/global` 아래에 포함되어 있으며, 아래 경로로 접근할 수 있습니다.

- `/global/login`
- `/global/orders`
- `/global/orders/detail`
- `/global/orders/new`
- `/global/users`
- `/global/users/detail`
- `/global/statistics`
- `/global/profile`
- `/global/settings`
- `/global/support`
- `/global/chat/personal`
- `/global/chat/room`
- `/global/billing/settlement/driver`
- `/global/billing/settlement/shipper`

## Getting Started

### Requirements

- JDK 17
- Oracle Database
- Gradle Wrapper
- Redis optional

### Run

```powershell
cd C:\ytheory\springPro\Backend\Barotruck
.\gradlew bootRun
```

### Test

```powershell
.\gradlew test
```

## Environment Variables

`src/main/resources/application.properties` 기준으로 아래 환경 변수를 사용합니다.

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JPA_DDL_AUTO=
JWT_SECRET_KEY=
GOOGLE_MAPS_KEY=
GOOGLE_PLACES_KEY=
GOOGLE_OAUTH_CLIENT_ID=
HASHIDS_SALT=
REDIS_HOST=
REDIS_PORT=
SMTP_HOST=
SMTP_PORT=
SMTP_USERNAME=
SMTP_PASSWORD=
CLOVA_INVOKE_URL=
CLOVA_SECRET_KEY=
GEMINI_API_KEY=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=
AWS_REGION=
SOLAPI_API_KEY=
SOLAPI_API_SECRET=
SOLAPI_FROM_NUMBER=
TOSS_BASE_URL=
TOSS_SECRET_KEY=
TOSS_BILLING_SECRET_KEY=
TOSS_CLIENT_KEY=
TOSS_SECURITY_KEY=
TOSS_WEBHOOK_SECRET=
TOSS_SUCCESS_URL=
TOSS_FAIL_URL=
TOSS_BILLING_SUCCESS_URL=
TOSS_BILLING_FAIL_URL=
PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED=
PAYMENT_PAYOUT_MOCK_ENABLED=
TOSS_PAYOUT_SECRET_KEY=
TOSS_PAYOUT_SECURITY_KEY=
```

## API Docs

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## Resources

- 설정 파일: `src/main/resources/application.properties`
- 초기 SQL: `src/main/resources/data.sql`
- Firebase 서비스 계정 기본 경로: `src/main/resources/firebase-service-account.json`
- Oracle 보조 스크립트: `scripts/db/oracle/*.sql`

## Notes

- 메인 클래스는 `SmartRoutePlannerApplication`이며 `@EnableScheduling`, `@EnableAsync`, `@EnableJpaAuditing`가 활성화되어 있습니다.
- WebSocket은 SockJS를 포함한 `/ws-stomp` 엔드포인트를 사용합니다.
- `docs/` 폴더는 현재 비어 있습니다.
- 민감한 키 파일과 시크릿 값은 배포 환경에서 외부 비밀 관리 방식으로 분리하는 것이 안전합니다.
