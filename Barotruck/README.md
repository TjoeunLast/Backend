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
    <td align="center"><img src="assets/icons/websocket.svg" alt="WebSocket" width="52" height="52"><br><strong>WebSocket</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="assets/icons/awss3.svg" alt="AWS S3" width="52" height="52"><br><strong>AWS S3</strong></td>
    <td align="center"><img src="assets/icons/firebase.svg" alt="Firebase" width="52" height="52"><br><strong>Firebase</strong></td>
    <td align="center"><img src="assets/icons/swagger.svg" alt="Swagger" width="52" height="52"><br><strong>Swagger</strong></td>
    <td align="center"><img src="assets/icons/jwt.svg" alt="JWT" width="52" height="52"><br><strong>JWT</strong></td>
    <td align="center"><img src="assets/icons/google.svg" alt="Google" width="52" height="52"><br><strong>Google APIs</strong></td>
  </tr>
</table>

</div>

## Overview

Barotruck 백엔드는 Spring Boot 기반의 통합 물류 서비스 서버입니다.  
하나의 애플리케이션 안에서 인증/회원 관리, 주문 등록과 배차, 운행 정산, 결제와 빌링, 채팅, 알림, 공지/리뷰/신고, OCR과 주소 보조 기능까지 함께 처리합니다.

이 프로젝트는 단순한 주문 CRUD 서버가 아닙니다.  
화주가 운송을 요청하고, 기사가 배차를 받아 운행을 수행하고, 운영자가 예외 상황을 관리하고, 결제와 정산까지 마무리되는 전체 물류 운영 흐름을 백엔드에서 책임집니다.

README를 처음 보는 사람도 빠르게 이해할 수 있도록, 아래는 "이 서비스가 실제로 무엇을 하는지" 중심으로 설명합니다.

## At a Glance

- 해결하려는 문제: 주문 접수, 기사 매칭, 운행 상태, 결제, 정산을 흩어진 수기 업무가 아니라 하나의 서비스 흐름으로 관리합니다.
- 주요 사용자: `SHIPPER`, `DRIVER`, `ADMIN`
- 핵심 흐름: 가입/인증 -> 주문 생성 -> 기사 매칭 -> 운행 진행 -> 결제 확인 -> 정산/지급
- 운영 기능: 실시간 채팅, 푸시 알림, 공지, 리뷰/신고, OCR, 주소/동네 해석
- 외부 연동: Toss Payments, AWS S3, Firebase, Google APIs, Solapi, Clova OCR

## Why This Project Feels Practical

- 주문 생성 이후의 실제 운영 단계까지 다룹니다.
- 자동 배차와 운영자 수동 개입이 함께 가능한 구조입니다.
- 결제 성공 기록만 남기는 것이 아니라 빌링키, 수수료 청구서, 기사 지급 상태까지 관리합니다.
- 관리자용 주문/사용자/결제 운영 API가 따로 분리되어 있어 실서비스 운영 구조를 설명하기 좋습니다.
- 채팅과 알림까지 포함되어 있어 업무 흐름이 중간에 끊기지 않습니다.

## User Roles

- `SHIPPER`: 주문 등록, 기사 선택, 결제, 빌링, 수수료 확인, 내 주문/매출 조회를 담당합니다.
- `DRIVER`: 추천 주문 조회, 오퍼 수락/거절, 운행 상태 변경, 도착 사진 업로드, 결제 확인, 지급 상태 조회를 담당합니다.
- `ADMIN`: 주문 운영, 강제 배차, 사용자 관리, 공지 관리, 분쟁/정산/지급 운영, 통계 조회를 담당합니다.

## Typical Journey

1. 사용자가 이메일/구글/SMS 기반으로 가입하거나 로그인하고, 역할별 프로필을 등록합니다.
2. 화주가 주문을 생성하고 예상 운임을 계산한 뒤 주문 이미지나 증빙 자료를 첨부합니다.
3. 시스템이 자동 배차를 실행하거나, 기사에게 오퍼를 제안하고, 필요하면 화주나 관리자가 직접 기사를 선택합니다.
4. 기사는 추천 주문을 확인하고 운행을 수락한 뒤, 위치와 운행 상태를 업데이트하고 도착 사진을 업로드합니다.
5. 화주는 결제를 완료하거나 Toss 결제를 진행하고, 기사는 결제를 확인하며 필요 시 분쟁을 등록할 수 있습니다.
6. 시스템과 관리자는 수수료 청구서, 빌링 계약, 기사 지급 상태, 정산 요약을 관리하며 운영 이슈를 처리합니다.

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

### External Integrations

- JWT Authentication
- AWS S3
- Firebase Admin SDK
- Toss Payments
- Google OAuth / Maps / Places
- Solapi SMS
- Naver Clova OCR

## Main Capabilities

아래는 "무엇이 구현되어 있는가"를 사람이 읽기 쉬운 흐름으로 정리한 섹션입니다.  
세부 API는 필요한 경우만 펼쳐서 볼 수 있도록 대표 엔드포인트 위주로 넣었습니다.

### Authentication & Account

서비스 진입, 계정 복구, 역할별 사용자 설정을 담당합니다.

- 이메일, Google, SMS 기반 인증 흐름이 함께 있습니다.
- 화주와 기사 프로필이 분리되어 있어 역할별 정보 구조가 명확합니다.
- 프로필 이미지, FCM 토큰, 닉네임 중복 확인, 비밀번호 변경 같은 실제 앱 운영 기능이 포함되어 있습니다.
- 관리자용 사용자 관리 API가 별도로 분리되어 있습니다.

<details>
  <summary>Representative APIs</summary>

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
- `/api/user/**`
- `/api/v1/shippers/**`
- `/api/v1/drivers/**`
- `/api/v1/admin/user/**`

</details>

### Orders & Dispatch

이 프로젝트의 중심 도메인으로, 주문 생성부터 기사 매칭과 운행 진행 상태까지 전체 운송 사이클을 관리합니다.

- 화주가 주문을 생성하고, 운임을 계산하고, 기사 후보를 보고, 최종 기사를 선택할 수 있습니다.
- 기사는 자신에게 맞는 주문을 추천받고, 제안된 오퍼를 수락하거나 거절할 수 있습니다.
- 운행 중에는 상태 변경, 위치 업데이트, 도착 사진 업로드 같은 실제 현장 흐름이 반영되어 있습니다.
- 관리자는 강제 배차, 강제 취소, 지역별 통계, 종합 통계까지 조회할 수 있습니다.

<details>
  <summary>Representative APIs</summary>

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `PUT /api/v1/orders/{orderId}`
- `GET /api/v1/orders/search`
- `GET /api/v1/orders/available`
- `GET /api/v1/orders/recommended`
- `GET /api/v1/orders/my-driving`
- `GET /api/v1/orders/my-shipper`
- `GET /api/v1/orders/my-revenue`
- `PATCH /api/v1/orders/{orderId}/accept`
- `GET /api/v1/orders/{orderId}/applicants`
- `POST /api/v1/orders/{orderId}/select-driver`
- `PATCH /api/v1/orders/{orderId}/cancel`
- `PATCH /api/v1/orders/{orderId}/status`
- `/api/v1/orders/{orderId}/image`
- `/api/v1/orders/{orderId}/arrival-photo`
- `POST /api/v1/dispatch/orders/{orderId}/run`
- `POST /api/v1/dispatch/orders/{orderId}/retry`
- `GET /api/v1/dispatch/orders/{orderId}/status`
- `GET /api/v1/dispatch/jobs/recent`
- `POST /api/v1/dispatch/jobs/{dispatchJobId}/force-match`
- `/api/v1/dispatch/driver/**`
- `/api/v1/dispatch/offers/{offerId}/accept`
- `/api/v1/dispatch/offers/{offerId}/reject`
- `/api/v1/admin/orders/**`

</details>

### Payments & Settlement

결제 성공 여부만 저장하는 수준이 아니라, 실제 운영에 필요한 수수료 정책, 빌링 계약, 기사 지급, 정산 추적까지 포함합니다.

- 화주는 예상 수수료를 확인하고 결제를 진행하거나, 빌링키 기반 결제 수단을 연결할 수 있습니다.
- 기사는 주문별 결제 확인과 지급 상태를 확인할 수 있습니다.
- 관리자 입장에서는 분쟁 처리, 지급 실행, 결제 상태 조회, 정산 요약까지 운영 가능한 구조입니다.
- 결제와 정산이 분리되어 있어 실제 정산 운영 시나리오를 설명하기 좋습니다.

<details>
  <summary>Representative APIs</summary>

- `POST /api/v1/payments/fee-preview`
- `POST /api/v1/payments/orders/{orderId}/mark-paid`
- `POST /api/v1/payments/orders/{orderId}/confirm`
- `POST /api/v1/payments/orders/{orderId}/disputes`
- `POST /api/v1/payments/orders/{orderId}/toss/prepare`
- `POST /api/v1/payments/orders/{orderId}/toss/confirm`
- `POST /api/v1/payments/webhooks/toss`
- `/api/v1/payments/billing/context`
- `/api/v1/payments/billing/agreements`
- `/api/v1/payments/billing/agreements/me`
- `/api/v1/payments/payouts/orders/{orderId}/status`
- `/api/v1/payments/fee-invoices/me`
- `/api/v1/payments/fee-invoices/{invoiceId}/mark-paid`
- `/api/admin/payment/**`
- `/api/v1/settlements/**`

</details>

### Chat, Notifications & Support

주문 운영을 보조하는 커뮤니케이션과 운영 지원 기능입니다.

- 1:1 채팅방과 메시지 히스토리를 제공해 화주-기사 또는 운영 커뮤니케이션 흐름을 지원합니다.
- Firebase 기반 푸시 알림과 알림 읽음 처리가 구현되어 있습니다.
- 공지, 리뷰, 신고 API가 분리되어 있어 운영/커뮤니티 기능도 담고 있습니다.
- OCR, 주소 검색, 동네 해석, 운송 증빙처럼 현장 업무를 돕는 기능이 포함됩니다.

<details>
  <summary>Representative APIs</summary>

- `/api/chat/room/personal/{targetId}`
- `/api/chat/room`
- `/api/chat/room/{roomId}`
- `/api/chat/room/{roomId}/leave`
- `/api/chat/room/{roomId}/messages`
- WebSocket STOMP: `/ws-stomp`, `/pub/**`, `/sub/**`
- `/api/notifications`
- `/api/notifications/{notificationId}/read`
- `/api/notices/**`
- `/api/admin/notices/**`
- `/api/reviews/**`
- `/api/reports/**`
- `/api/ocr/**`
- `/api/neighborhoods/**`
- `/api/proof/**`

</details>

### Admin Operations

운영자가 서비스 전체를 관리할 수 있도록 주문, 사용자, 공지, 결제, 정산 영역의 관리자 API가 분리되어 있습니다.

- 사용자 상태 변경과 복구, 주문 강제 처리, 공지 관리, 분쟁 처리, 지급 실행 같은 운영 업무를 한 백엔드에서 다룹니다.
- 지역별 통계, 종합 요약, 결제 상태 조회, 정산 상태 조회까지 포함되어 있어 운영 도구의 성격도 강합니다.

<details>
  <summary>Representative APIs</summary>

- `/api/v1/admin/user/**`
- `/api/v1/admin/orders/**`
- `/api/admin/notices/**`
- `/api/admin/payment/**`

</details>

## Getting Started

### Requirements

- JDK 17
- Oracle Database
- Gradle Wrapper

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
- `spring-boot-starter-data-redis` 의존성과 `spring.data.redis.*` 설정 키는 남아 있지만, 현재 애플리케이션 로직에서는 RedisTemplate/캐시/Redis 저장소를 사용하지 않습니다.
- 이메일 인증 코드는 Redis TTL이 아니라 DB 테이블에 저장되며, `EmailAuthService`의 스케줄러가 만료 데이터를 정리합니다.
- `docs/` 폴더는 현재 비어 있습니다.
- 민감한 키 파일과 시크릿 값은 배포 환경에서 외부 비밀 관리 방식으로 분리하는 것이 안전합니다.
