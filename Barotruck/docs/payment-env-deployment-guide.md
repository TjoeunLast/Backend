# Payment Environment / Deployment Guide

기준일: 2026-03-10

이 문서는 `application.properties`에 남길 결제 설정 키와, local / dev / staging / prod 배포 시 실제로 주입해야 하는 env를 정리한 문서다.

원칙:

1. Git에는 실제 비밀값을 커밋하지 않는다.
2. test key와 live key를 같은 런타임 인스턴스에 섞지 않는다.
3. env 누락 시에는 가능하면 `fail closed`로 동작하게 하고, mock 사용은 명시적으로 켠다.

---

## 1. 이번 기준으로 정리한 key

### 1.1 Toss 결제 / billing

- `payment.toss.base-url`
- `payment.toss.secret-key`
- `payment.toss.billing.secret-key`
- `payment.toss.client-key`
- `payment.toss.security-key`
- `payment.toss.webhook-secret`
- `payment.toss.redirect.success-url`
- `payment.toss.redirect.fail-url`
- `payment.toss.billing.redirect.success-url`
- `payment.toss.billing.redirect.fail-url`
- `payment.toss.retry.max-attempts`
- `payment.toss.expire.cron`
- `payment.toss.retry.cron`

### 1.2 fee auto-charge

- `payment.fee-auto-charge.mock-enabled`
- `payment.fee-invoice.generate.cron`
- `payment.fee-invoice.auto-charge.cron`

### 1.3 payout

- `payment.payout.mock-enabled`
- `payment.payout.cron`
- `payment.payout.sync-cron`
- `payment.toss.payout.secret-key`
- `payment.toss.payout.security-key`
- `payment.toss.payout.seller-path`
- `payment.toss.payout.path`
- `payment.toss.payout.schedule-type`
- `payment.toss.payout.timezone`
- `payment.toss.payout.scheduled-hour`
- `payment.toss.payout.scheduled-minute`

---

## 2. application.properties 반영 기준

현재 설정 기준 핵심 포인트:

1. `payment.fee-auto-charge.mock-enabled` 기본값은 `false`
2. `payment.payout.mock-enabled` 기본값은 `false`
3. `payment.toss.billing.secret-key`는 `TOSS_BILLING_SECRET_KEY`가 없으면 `TOSS_SECRET_KEY` fallback
4. `payment.toss.payout.secret-key`는 `TOSS_PAYOUT_SECRET_KEY`가 없으면 `TOSS_SECRET_KEY` fallback
5. `payment.toss.payout.security-key`는 `TOSS_PAYOUT_SECURITY_KEY`가 없으면 `payment.toss.security-key` fallback
6. `payment.payout.cron`은 실제 코드에서 사용 중이므로 env로 관리한다

의도:

- auto-charge는 env를 빼먹었을 때 더미 성공으로 지나가지 않게 한다
- payout은 env를 빼먹었을 때 실제 gateway bean이 선택되더라도 비밀키 누락으로 조용히 성공하지 않고 실패 이유가 남게 한다

---

## 3. 필요한 env 목록

### 3.1 공통 런타임

| Env | Required | 비고 |
| --- | --- | --- |
| `DB_URL` | Y | Oracle JDBC URL |
| `DB_USERNAME` | Y | DB 계정 |
| `DB_PASSWORD` | Y | DB 비밀번호 |
| `JWT_SECRET_KEY` | Y | JWT 서명키 |
| `JPA_DDL_AUTO` | N | 기본 `update`, 운영에서는 명시 권장 |

### 3.2 Toss 결제 / billing

| Env | Required | 비고 |
| --- | --- | --- |
| `TOSS_BASE_URL` | N | 기본 `https://api.tosspayments.com` |
| `TOSS_CLIENT_KEY` | Y | 결제 prepare와 billing context에서 사용 |
| `TOSS_SECRET_KEY` | Y | confirm / lookup / cancel 기본 secret |
| `TOSS_BILLING_SECRET_KEY` | N | billing 전용 secret, 없으면 `TOSS_SECRET_KEY` fallback |
| `TOSS_WEBHOOK_SECRET` | N | webhook 검증을 켜려면 필요 |
| `TOSS_SUCCESS_URL` | N | 일반 결제 성공 redirect |
| `TOSS_FAIL_URL` | N | 일반 결제 실패 redirect |
| `TOSS_BILLING_SUCCESS_URL` | N | billing 성공 redirect |
| `TOSS_BILLING_FAIL_URL` | N | billing 실패 redirect |
| `PAYMENT_TOSS_RETRY_MAX_ATTEMPTS` | N | 기본 `5` |
| `PAYMENT_TOSS_EXPIRE_CRON` | N | 기본 `0 */5 * * * *` |
| `PAYMENT_TOSS_RETRY_CRON` | N | 기본 `0 */5 * * * *` |

### 3.3 fee auto-charge

| Env | Required | 비고 |
| --- | --- | --- |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | N | 기본 `false`, local/dev에서 mock이면 명시적으로 `true` |
| `PAYMENT_FEE_INVOICE_GENERATE_CRON` | N | 기본 `0 5 1 1 * *` |
| `PAYMENT_FEE_INVOICE_AUTO_CHARGE_CRON` | N | 기본 `0 20 1 * * *` |

### 3.4 payout

| Env | Required | 비고 |
| --- | --- | --- |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | N | 기본 `false`, mock이면 명시적으로 `true` |
| `PAYMENT_PAYOUT_CRON` | N | 기본 `0 0 4 * * *` |
| `PAYMENT_PAYOUT_SYNC_CRON` | N | 기본 `0 */15 * * * *` |
| `TOSS_PAYOUT_SECRET_KEY` | N | payout 전용 secret, 없으면 `TOSS_SECRET_KEY` fallback |
| `TOSS_PAYOUT_SECURITY_KEY` | N | payout 암호화 키, 없으면 `TOSS_SECURITY_KEY` fallback |
| `TOSS_SECURITY_KEY` | N | legacy/common fallback. 가능하면 `TOSS_PAYOUT_SECURITY_KEY` 사용 권장 |
| `TOSS_PAYOUT_SELLER_PATH` | N | 기본 `/v2/sellers` |
| `TOSS_PAYOUT_PATH` | N | 기본 `/v2/payouts` |
| `TOSS_PAYOUT_SCHEDULE_TYPE` | N | 기본 `AUTO` |
| `TOSS_PAYOUT_TIMEZONE` | N | 기본 `Asia/Seoul` |
| `TOSS_PAYOUT_SCHEDULED_HOUR` | N | 기본 `9` |
| `TOSS_PAYOUT_SCHEDULED_MINUTE` | N | 기본 `0` |

---

## 4. env 누락 시 동작 / 실패 방식

| 설정 | 누락 시 동작 | 실제 실패 지점 |
| --- | --- | --- |
| `TOSS_CLIENT_KEY` | 값이 비어 있음 | 일반 결제 `prepare`에서 `payment.toss.client-key is required for real toss checkout` 예외. billing context는 빈 값 반환 가능 |
| `TOSS_SECRET_KEY` | 값이 비어 있음 | 일반 결제 `confirm`, lookup, cancel에서 런타임 실패 |
| `TOSS_BILLING_SECRET_KEY` | `TOSS_SECRET_KEY`로 fallback | 둘 다 없으면 billing issue / charge / delete 실패 |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | 기본 `false` | 실클라이언트가 선택됨. billing agreement 또는 secret이 없으면 auto-charge attempt가 실패로 기록되고 invoice는 `OVERDUE`로 남음 |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | 기본 `false` | 실 payout client가 선택됨. payout secret / security key가 없으면 요청 시 실패 이유가 남음 |
| `TOSS_PAYOUT_SECRET_KEY` | `TOSS_SECRET_KEY`로 fallback | 둘 다 없으면 payout 요청 / 상태조회 실패 |
| `TOSS_PAYOUT_SECURITY_KEY` | `TOSS_SECURITY_KEY`로 fallback | 둘 다 없거나 형식이 잘못되면 payout 요청 실패. 64 hex 형식 필요 |
| `TOSS_WEBHOOK_SECRET` | 검증 비활성 | 값이 비어 있으면 webhook secret 검증을 건너뜀 |
| `TOSS_SUCCESS_URL`, `TOSS_FAIL_URL` | 코드 기본값 사용 | 값이 없더라도 서버는 기동하지만, 클라이언트 연동 방식과 안 맞으면 실제 결제 redirect에서 문제 발생 가능 |
| `TOSS_BILLING_SUCCESS_URL`, `TOSS_BILLING_FAIL_URL` | 코드 기본값 사용 | billing redirect가 앱/웹 연동 방식과 안 맞으면 등록 플로우에서 실패 가능 |
| `JPA_DDL_AUTO=update` | JPA 엔티티 기준으로 스키마 생성/보정 시도 | local/dev 기본값 |
| `JPA_DDL_AUTO=validate` | DDL 검증 수행 | 운영에서 테이블/컬럼 누락이면 앱 기동 시 바로 실패 |

추가 메모:

- webhook secret은 “없으면 실패”가 아니라 “없으면 검증 생략”이다.
- redirect URL은 서버 기동 실패 포인트가 아니라, 실제 프론트 연동 시 런타임 장애 포인트다.
- auto-charge는 현재 기본값을 `false`로 두어, env 누락 시 더미 성공으로 조용히 지나가지 않게 했다.

---

## 5. 환경별 권장값

### 5.1 local

목표:

- 개발 편의 우선
- 비밀값이 없더라도 서버는 뜨게 하되, 실청구/실지급은 기본적으로 막는다

권장:

| 항목 | 권장값 |
| --- | --- |
| `TOSS_CLIENT_KEY` | test key |
| `TOSS_SECRET_KEY` | test key |
| `TOSS_BILLING_SECRET_KEY` | 비우거나 test billing key |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | `true` |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | `true` |
| `TOSS_SUCCESS_URL`, `TOSS_FAIL_URL` | local callback 또는 테스트용 redirect |
| `TOSS_BILLING_SUCCESS_URL`, `TOSS_BILLING_FAIL_URL` | local callback 또는 테스트용 redirect |

### 5.2 dev

목표:

- 기능 개발과 API 연동 검증
- 필요한 경우에만 실 PG test call 사용

권장:

| 항목 | 권장값 |
| --- | --- |
| `TOSS_CLIENT_KEY` | test key |
| `TOSS_SECRET_KEY` | test key |
| `TOSS_BILLING_SECRET_KEY` | test key 또는 비움 |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | 기본 `true`, billing/charge 검증 시만 `false` |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | 기본 `true`, payout 검증 시만 `false` |
| redirect URLs | dev 앱/웹 경로로 명시 |

### 5.3 staging

목표:

- prod와 최대한 비슷한 설정
- 실운영 전 검증

권장:

| 항목 | 권장값 |
| --- | --- |
| `TOSS_CLIENT_KEY` | test key |
| `TOSS_SECRET_KEY` | test key |
| `TOSS_BILLING_SECRET_KEY` | test key 또는 비움 |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | `false` |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | `false` |
| `TOSS_WEBHOOK_SECRET` | 설정 |
| redirect URLs | staging 도메인 기준으로 명시 |

주의:

- staging에서 payout / auto-charge 실호출을 켜면 반드시 test 데이터와 test seller 기준으로만 검증한다.

### 5.4 prod

목표:

- live key만 사용
- mock 경로 금지

권장:

| 항목 | 권장값 |
| --- | --- |
| `TOSS_CLIENT_KEY` | live key |
| `TOSS_SECRET_KEY` | live key |
| `TOSS_BILLING_SECRET_KEY` | live key 또는 운영 분리 키 |
| `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` | `false` |
| `PAYMENT_PAYOUT_MOCK_ENABLED` | `false` |
| `TOSS_PAYOUT_SECRET_KEY` | live payout key |
| `TOSS_PAYOUT_SECURITY_KEY` | live payout security key |
| `TOSS_WEBHOOK_SECRET` | 반드시 설정 |
| redirect URLs | prod 앱/웹 경로로 명시 |

---

## 6. redirect URL 가이드

원칙:

1. 일반 결제와 billing은 별도 URL을 쓴다.
2. local 기본값에 의존하지 말고 환경별로 명시한다.
3. 앱 딥링크를 쓰든 웹 callback을 쓰든, 현재 프론트 구현 방식과 동일한 URL을 맞춘다.

예시 분리:

- 일반 결제 성공: `barotruck://pay/success`
- 일반 결제 실패: `barotruck://pay/fail`
- billing 성공: `barotruck://billing/success`
- billing 실패: `barotruck://billing/fail`

운영 권장:

- staging/prod는 실제 앱 또는 웹 callback URL을 env로 명시하고, 배포 후 샘플 결제로 직접 확인한다.

---

## 7. 스키마 반영 기준

신규 payment 스키마의 기준은 SQL 스크립트가 아니라 JPA 엔티티다.

적용 원칙:

1. local/dev는 `JPA_DDL_AUTO=update`로 서버를 기동한다.
2. 이때 Hibernate가 payment 도메인 엔티티를 기준으로 테이블/컬럼을 생성 또는 보정한다.
3. 운영은 `JPA_DDL_AUTO=validate`로 기동해 실제 스키마 누락을 검증한다.

기준 엔티티 예시:

1. `ShipperBillingAgreement`
2. `FeeAutoChargeAttempt`
3. `PaymentGatewayTransaction`

즉, 이 저장소에서 신규 payment 테이블 생성 기준은 `src/main/java/.../domain/payment/domain` 아래 엔티티다.

---

## 8. 배포 체크리스트

### 8.1 공통

- [ ] 실제 비밀값이 Git diff에 포함되지 않았는지 확인
- [ ] secret manager / CI 변수에 key가 환경별로 분리되어 있는지 확인
- [ ] local/dev는 `JPA_DDL_AUTO=update`로 신규 payment 테이블/컬럼이 생성되는지 확인
- [ ] 운영은 `JPA_DDL_AUTO=validate` 기준으로 DDL 적용 여부 확인
- [ ] `./gradlew.bat compileJava` 통과 확인

### 8.2 billing

- [ ] `TOSS_CLIENT_KEY` 설정 확인
- [ ] `TOSS_SECRET_KEY` 또는 `TOSS_BILLING_SECRET_KEY` 설정 확인
- [ ] `TOSS_BILLING_SUCCESS_URL`, `TOSS_BILLING_FAIL_URL` 설정 확인
- [ ] billing 등록/조회/해지 API를 test key 기준으로 최소 1회 검증

### 8.3 auto-charge

- [ ] `PAYMENT_FEE_AUTO_CHARGE_MOCK_ENABLED` 값을 환경별로 명시
- [ ] staging/prod는 `false`인지 확인
- [ ] billing agreement 없는 상태에서 실패가 원장에 남는지 확인
- [ ] 실청구를 켠 환경에서는 test invoice로 먼저 검증

### 8.4 payout

- [ ] `PAYMENT_PAYOUT_MOCK_ENABLED` 값을 환경별로 명시
- [ ] `PAYMENT_PAYOUT_CRON`, `PAYMENT_PAYOUT_SYNC_CRON` 확인
- [ ] `TOSS_PAYOUT_SECRET_KEY` 설정 확인
- [ ] `TOSS_PAYOUT_SECURITY_KEY` 형식이 64 hex인지 확인
- [ ] seller 등록 / payout status sync를 test 데이터로 검증

### 8.5 webhook

- [ ] `TOSS_WEBHOOK_SECRET` 설정 여부 확인
- [ ] Toss 콘솔 webhook 설정과 서버 env가 일치하는지 확인
- [ ] `payout.changed`, `seller.changed`, payment 관련 sample payload로 검증

---

## 9. 운영 메모

- `payment.fee-auto-charge.mock-enabled`는 누락 시 기본 `false`다. local/dev에서 mock이 필요하면 env를 명시적으로 넣어야 한다.
- `payment.payout.mock-enabled`는 누락 시 기본 `false`다. payout mock이 필요하면 env를 명시적으로 넣어야 한다.
- `TOSS_PAYOUT_SECRET_KEY`, `TOSS_PAYOUT_SECURITY_KEY`를 따로 두지 않으면 fallback이 가능하지만, 운영에서는 payout 전용 값 분리를 권장한다.
- `payment.payout.cron`은 코드에서 실제 사용 중이므로 운영 배포 문서와 CI 변수 정의에 포함해야 한다.
