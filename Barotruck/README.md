<div align="center">

![Barotruck Banner](https://capsule-render.vercel.app/api?type=waving&color=0:0F172A,50:1D4ED8,100:16A34A&height=220&section=header&text=Barotruck%20Backend&fontSize=42&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Spring%20Boot%20Logistics%20Platform%20Backend&descAlignY=58&descSize=16)

# Barotruck Backend

화주, 기사, 관리자 역할을 기준으로 주문, 배차, 결제, 정산, 채팅, 알림을 처리하는 물류 플랫폼 백엔드입니다.

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

## 프로젝트 소개

Barotruck는 단순히 주문만 저장하는 서버가 아니라, 물류 서비스가 실제로 굴러가기 위해 필요한 운영 흐름 전체를 다루는 백엔드입니다.

- 화주는 주문을 생성하고 예상 운임을 계산하고, 기사 후보를 확인한 뒤 최종 배차를 확정할 수 있습니다.
- 기사는 자신에게 맞는 주문을 추천받고, 배차 오퍼를 수락하거나 거절하고, 운행 상태와 위치를 업데이트할 수 있습니다.
- 관리자는 주문 현황, 취소 이력, 지역별 통계, 결제 상태, 정산 상태를 확인하고 필요한 경우 강제 배차나 운영 개입을 수행할 수 있습니다.
- 결제는 단순 결제 기록에 그치지 않고 Toss 결제 준비/승인, 빌링키 발급, 수수료 청구서, 기사 지급 추적까지 포함합니다.
- 채팅, 푸시 알림, 공지, 리뷰, 신고, OCR, 주소/동네 해석 기능까지 포함해 실제 서비스 운영에 필요한 주변 기능도 함께 제공합니다.

### 한눈에 보기

- 주요 사용자: `SHIPPER`, `DRIVER`, `ADMIN`
- 핵심 흐름: 가입/인증 -> 주문 생성 -> 기사 매칭 -> 운행 진행 -> 결제 확인 -> 정산/지급
- 운영 기능: 실시간 채팅, 푸시 알림, 공지, 리뷰/신고, OCR, 주소/동네 해석
- API 문서: `/swagger-ui.html`, `/v3/api-docs`

## 팀원 구성

저장소 커밋 이력 기준 주요 기여자입니다.

| 이름 | GitHub ID | 비고 |
| :------: | :------: | :------ |
| lealle | `lealle` | 주요 초기 구조 및 도메인 구현 기여 |
| Ytheory | `yysi0580` | 주문, 결제, 정산, 문서화 기여 |
| suji9078 | `suji9078` | 기능 확장 및 유지보수 기여 |
| yulim1115 | `yulim1115` | 기능 개발 기여 |

## 1. 개발 환경

- Language: Java 17
- Framework: Spring Boot 3.3.5
- Security: Spring Security, JWT
- Data: Oracle Database, Spring Data JPA, JDBC, Querydsl
- Realtime: WebSocket, STOMP, SockJS
- Storage: AWS S3
- Notification: Firebase Admin SDK
- External APIs: Toss Payments, Google OAuth/Maps/Places, Solapi SMS, Naver Clova OCR
- Documentation: Swagger UI, OpenAPI
- Version Control: Git, GitHub

## 2. 채택한 개발 기술과 브랜치 전략

### Spring Boot, Security

- 인증, 인가, 예외 처리, API 계층을 한 애플리케이션 안에서 관리하기 위해 Spring Boot와 Spring Security를 사용했습니다.
- 사용자 역할이 `SHIPPER`, `DRIVER`, `ADMIN`으로 명확히 나뉘어 있어 권한별 API 접근 제어가 중요한 구조입니다.
- JWT 기반 인증과 Google 로그인, 이메일/SMS 인증 흐름을 함께 지원합니다.

### JPA, JDBC, Querydsl

- 주문, 배차, 결제, 정산처럼 관계가 많은 도메인을 안정적으로 관리하기 위해 JPA를 사용했습니다.
- 검색, 통계, 조건 기반 조회는 Querydsl과 조회 전용 서비스 계층으로 분리해 유지보수성을 높였습니다.
- 결제/정산 영역처럼 운영성 조회가 많은 기능은 별도 Query 서비스와 상태 조회 DTO를 두는 구조를 사용합니다.

### WebSocket, Firebase

- 채팅은 REST 조회 + STOMP 실시간 메시징 구조를 사용합니다.
- 읽지 않은 알림과 이벤트 전달은 Firebase 기반 푸시 알림과 함께 운영됩니다.

### Toss Payments, AWS S3

- 결제는 Toss 준비/승인/웹훅 처리 흐름을 포함합니다.
- 이미지와 증빙 파일은 AWS S3 업로드 구조를 사용합니다.

### 브랜치 전략

- 저장소 기준으로 `main` 브랜치와 개인 작업 브랜치(`ytheory`, `yangshipper5` 등)를 병행하는 방식으로 작업한 흔적이 확인됩니다.
- 즉, 하나의 장기 메인 브랜치 위에 개인/기능 브랜치를 분기해 작업하고 병합하는 협업 방식에 가깝습니다.

## 3. 프로젝트 구조

```text
Barotruck
├── src/main/java/com/example/project
│   ├── domain
│   │   ├── order
│   │   ├── dispatch
│   │   ├── payment
│   │   ├── settlement
│   │   ├── chat
│   │   ├── notification
│   │   ├── notice
│   │   ├── review
│   │   └── proof
│   ├── member
│   ├── security
│   └── global
│       ├── api
│       ├── config
│       ├── email
│       ├── fcm
│       ├── image
│       ├── neighborhood
│       └── toss
├── src/main/resources
│   ├── application.properties
│   ├── data.sql
│   └── firebase-service-account.json
├── scripts
├── docs
└── build.gradle
```

## 4. 역할 분담

백엔드 기준으로는 아래와 같은 책임 분리가 보입니다.

### 인증/회원 관리

- 회원가입, 로그인, JWT 재발급
- Google 로그인, 이메일/SMS 인증
- 사용자 프로필, 기사/화주 전용 프로필, 관리자 사용자 관리

### 주문/배차

- 주문 생성, 수정, 검색, 이미지/도착 사진 관리
- 기사 추천, 오퍼 수락/거절, 자동 배차, 강제 배차
- 운행 상태 변경, 기사 위치/가용 상태 업데이트

### 결제/정산

- 운송 결제 확인, 수수료 미리보기
- Toss 결제 준비/승인/웹훅 처리
- 빌링키, 수수료 청구서, 기사 지급, 정산 상태 관리

### 커뮤니케이션/운영 지원

- 채팅방, 채팅 메시지, 실시간 WebSocket
- 푸시 알림, 공지, 리뷰, 신고
- OCR, 주소 검색, 동네 해석, 운송 증빙

## 5. 개발 기간 및 작업 관리

### 개발 기간

- 저장소에서 확인되는 작업 기간: `2026-02-02` ~ `2026-03-21`

### 작업 관리

- GitHub 원격 저장소: `https://github.com/TjoeunLast/Backend.git`
- 버전 관리는 Git/GitHub를 사용합니다.
- 개인/작업 브랜치를 기반으로 기능 개발 후 메인 브랜치에 반영하는 흐름이 보입니다.
- API 문서는 Swagger UI와 OpenAPI JSON으로 확인할 수 있습니다.

## 6. 신경 쓴 부분

- 역할 기반 흐름: 화주, 기사, 관리자별로 필요한 기능과 권한이 다르기 때문에 API 경계를 분리했습니다.
- 운영형 배차 구조: 자동 배차만 있는 것이 아니라, 오퍼, 재시도, 강제 매칭, 관리자 개입 흐름까지 포함합니다.
- 결제와 정산의 분리: 결제 성공 여부와 별개로 수수료, 빌링, 지급, 정산 상태를 따로 관리합니다.
- 실시간 커뮤니케이션: 채팅과 알림이 주문/배차/운행 흐름과 자연스럽게 연결되도록 구성되어 있습니다.
- 실사용 기준 문서화: Redis 의존성과 설정은 남아 있지만, 현재 핵심 로직은 Oracle과 애플리케이션 서비스 계층 중심으로 동작하므로 README에는 실사용 기준만 반영했습니다.

## 7. 주요 기능

### [회원가입 / 인증]

- 이메일 회원가입, 로그인, JWT 재발급을 지원합니다.
- Google 로그인, 이메일 인증, SMS 인증 흐름을 함께 제공합니다.
- 비밀번호 재설정과 이메일 찾기 API도 포함되어 있습니다.

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

</details>

### [회원 / 프로필 관리]

- 사용자 기본 프로필 수정, 비밀번호 변경, 탈퇴/복구를 지원합니다.
- 프로필 이미지 업로드/조회/삭제와 FCM 토큰 저장이 가능합니다.
- 기사/화주 전용 프로필과 차량번호/사업자번호 확인 기능이 분리되어 있습니다.

<details>
  <summary>Representative APIs</summary>

- `/api/user/**`
- `/api/v1/shippers/**`
- `/api/v1/drivers/**`
- `/api/v1/admin/user/**`

</details>

### [주문 생성 / 배차]

- 화주는 주문을 생성하고 예상 운임을 계산하며 기사 후보를 확인할 수 있습니다.
- 기사는 배차 가능한 주문과 추천 주문을 조회하고, 주문을 수락할 수 있습니다.
- 자동 배차 실행, 재시도, 주문별 배차 상태 조회, 관리자 강제 매칭이 구현되어 있습니다.

<details>
  <summary>Representative APIs</summary>

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `PUT /api/v1/orders/{orderId}`
- `GET /api/v1/orders/search`
- `GET /api/v1/orders/available`
- `GET /api/v1/orders/recommended`
- `PATCH /api/v1/orders/{orderId}/accept`
- `GET /api/v1/orders/{orderId}/applicants`
- `POST /api/v1/orders/{orderId}/select-driver`
- `POST /api/v1/dispatch/orders/{orderId}/run`
- `POST /api/v1/dispatch/orders/{orderId}/retry`
- `GET /api/v1/dispatch/orders/{orderId}/status`
- `POST /api/v1/dispatch/jobs/{dispatchJobId}/force-match`

</details>

### [운행 진행]

- 기사는 현재 운행 중인 주문 목록을 조회할 수 있습니다.
- 운행 상태 변경, 위치 갱신, 도착 사진 업로드가 가능합니다.
- 주문 이미지와 도착 사진은 별도 파일 관리 흐름을 가집니다.

<details>
  <summary>Representative APIs</summary>

- `GET /api/v1/orders/my-driving`
- `PATCH /api/v1/orders/{orderId}/status`
- `/api/v1/orders/{orderId}/image`
- `/api/v1/orders/{orderId}/arrival-photo`
- `/api/v1/dispatch/driver/**`
- `/api/v1/dispatch/offers/{offerId}/accept`
- `/api/v1/dispatch/offers/{offerId}/reject`

</details>

### [결제 / 정산]

- 화주는 수수료를 미리 확인하고 주문 결제를 진행할 수 있습니다.
- Toss 결제 준비/승인/웹훅 처리와 빌링키 발급/조회/해지가 구현되어 있습니다.
- 기사는 지급 상태를 확인할 수 있고, 관리자는 수수료 청구서, 기사 지급, 정산 상태를 운영할 수 있습니다.

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

### [채팅 / 알림 / 운영 지원]

- 1:1 채팅방 생성과 채팅 히스토리 조회가 가능합니다.
- WebSocket STOMP 기반 실시간 메시지 송수신을 지원합니다.
- 푸시 알림, 공지, 리뷰, 신고, OCR, 주소 검색, 운송 증빙 기능이 포함됩니다.

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

### [관리자 운영]

- 주문, 사용자, 공지, 결제, 정산 영역의 관리자 API가 분리되어 있습니다.
- 강제 배차, 강제 취소, 사용자 상태 변경, 지급 실행, 결제 상태 조회, 통계 조회 같은 운영 업무를 지원합니다.

<details>
  <summary>Representative APIs</summary>

- `/api/v1/admin/user/**`
- `/api/v1/admin/orders/**`
- `/api/admin/notices/**`
- `/api/admin/payment/**`

</details>

## 8. API Docs

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## 9. 실행 방법

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

## 10. 환경 변수

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

## 11. 참고 사항

- 메인 클래스는 `SmartRoutePlannerApplication`이며 `@EnableScheduling`, `@EnableAsync`, `@EnableJpaAuditing`가 활성화되어 있습니다.
- WebSocket은 SockJS를 포함한 `/ws-stomp` 엔드포인트를 사용합니다.
- Firebase 서비스 계정 기본 경로는 `src/main/resources/firebase-service-account.json`입니다.
- 초기 SQL은 `src/main/resources/data.sql`에 있습니다.
- `spring-boot-starter-data-redis` 의존성과 `spring.data.redis.*` 설정 키는 남아 있지만, 현재 애플리케이션 로직에서는 RedisTemplate/캐시/Redis 저장소를 사용하지 않습니다.
- 이메일 인증 코드는 Redis TTL이 아니라 DB 테이블에 저장되며, `EmailAuthService`의 스케줄러가 만료 데이터를 정리합니다.
- `docs/` 폴더는 현재 비어 있습니다.
- 민감한 키 파일과 시크릿 값은 배포 환경에서 외부 비밀 관리 방식으로 분리하는 것이 안전합니다.
