# Payment Test App QA Checklist

기준일: 2026-03-10

이 문서는 테스트 앱에서 `결제 -> 내부 상태 반영 -> 정산 -> 지급 -> 차주 수령 확인`을 한 번에 검증할 때 QA 담당자가 바로 따라 쓸 수 있는 실행 체크리스트다.

상위 기준 문서:

1. `docs/toss-payment-test-app-plan.md`
2. `docs/payment-postman-test-cases.md`
3. `docs/toss-payment-gap-review.md`
4. `docs/payment-logic.md`

---

## 1. 실행 타입 선택

아래 둘 중 하나를 먼저 고른다.

- [ ] `REAL_TOSS_PAYMENT_E2E`
- [ ] `MANUAL_MARK_PAID_E2E`

선택 기준:

1. Toss test key와 결제창이 정상 준비돼 있으면 `REAL_TOSS_PAYMENT_E2E`
2. 결제창 검증이 아니라 정산/지급 파이프라인만 먼저 확인하면 `MANUAL_MARK_PAID_E2E`

주의:

- 두 타입의 결과는 같은 시트에 섞지 않는다.
- `REAL_TOSS_PAYMENT_E2E`에서는 `paymentKey`, `pgOrderId`를 반드시 남긴다.
- `MANUAL_MARK_PAID_E2E`에서는 `paymentKey`, `pgOrderId`가 비어 있을 수 있다.

---

## 2. 테스트 메타데이터

| 항목 | 기록값 |
| --- | --- |
| 실행 일시 |  |
| QA 담당자 |  |
| 서버 URL |  |
| 앱 빌드 버전 |  |
| 실행 타입 |  |
| 주문 ID |  |
| 화주 userId/email |  |
| 차주 userId/email |  |
| 관리자 계정 |  |
| 비고 |  |

---

## 3. 사전 체크

| ID | 체크 항목 | 기대 결과 | 확인 |
| --- | --- | --- | --- |
| PRE-01 | 테스트 앱 `환경 점검` 섹션에서 서버 주소 확인 | 올바른 `baseUrl` 표시 | [ ] |
| PRE-02 | 테스트 앱 `환경 점검` 섹션에서 Toss 결제 키 상태 확인 | `ready` 또는 동등한 정상 상태 | [ ] |
| PRE-03 | 테스트 앱 `환경 점검` 섹션에서 Toss payout 키 상태 확인 | `ready` 또는 동등한 정상 상태 | [ ] |
| PRE-04 | 테스트 앱 `계정 전환` 섹션에서 SHIPPER / DRIVER / ADMIN 계정이 모두 준비됨 | 세 actor 전환 가능 | [ ] |
| PRE-05 | 테스트 앱 `시나리오 준비` 섹션에서 주문 선택 또는 테스트 주문 자동 준비 | 검증 대상 `orderId` 확보 | [ ] |
| PRE-06 | 선택한 주문이 운송 완료 이후 상태임 | 결제 시작 가능 | [ ] |
| PRE-07 | 차주 계좌/셀러 정보가 payout 테스트 가능한 상태임 | 지급 단계 진행 가능 | [ ] |
| PRE-08 | 이전 지급 아이템/실패 데이터가 이번 실행을 오염시키지 않음 | 새 실행으로 판단 가능 | [ ] |

---

## 4. 본 실행 체크리스트

| ID | Actor | 앱 섹션 | 액션 | 기대 결과 | 필수 기록 | 확인 |
| --- | --- | --- | --- | --- | --- | --- |
| RUN-01 | SHIPPER | 환경 점검 | 서버/키/계정 상태 다시 확인 | 실행 중 경로와 계정이 확정됨 | 서버 URL, actor | [ ] |
| RUN-02 | SHIPPER | 화주 결제 | `billing context` 조회 | `clientKey`, `customerKey`, success/fail URL 표시 | `customerKey` | [ ] |
| RUN-03 | SHIPPER | 화주 결제 | `billing agreement me` 조회 | 현재 agreement 상태 확인 가능 | agreementId, status | [ ] |
| RUN-04 | SHIPPER | 화주 결제 | 필요 시 billing 등록 실행 | agreement가 `ACTIVE` 또는 테스트 목적상 신규 발급 완료 | agreementId, card mask | [ ] |
| RUN-05A | SHIPPER | 화주 결제 | `Toss prepare` 실행 | `pgOrderId`, `amount`, `successUrl`, `failUrl` 생성 | `pgOrderId`, `amount` | [ ] |
| RUN-05B | SHIPPER | 화주 결제 | Toss 결제창 결제 후 confirm 완료 | `TransportPayment=PAID`, `Order=PAID`, `Settlement=READY` | `paymentKey`, confirm 시각 | [ ] |
| RUN-05C | SHIPPER | 화주 결제 | 수동 `mark-paid` 실행 | `TransportPayment=PAID`, `Order=PAID`, `Settlement=READY` | proofUrl 또는 수동 결제 메모 | [ ] |
| RUN-06 | ADMIN | 관리자 지급 또는 운영 조회 | 주문별 payment snapshot 확인 | gateway/internal 상태가 기대값과 일치 | tx status, payment status | [ ] |
| RUN-07 | DRIVER | 차주 확인 | `driver confirm` 실행 | `TransportPayment=CONFIRMED`, `Order=CONFIRMED`, `Settlement=COMPLETED` | confirm 시각 | [ ] |
| RUN-08 | ADMIN | 관리자 지급 | `payout request` 실행 | `DriverPayoutItem` 생성, `REQUESTED` 또는 `COMPLETED` | itemId, payoutRef | [ ] |
| RUN-09 | ADMIN | 관리자 지급 | `payout item status` 조회 | `sellerStatus`, `failureReason`, webhook 요약 표시 | payout status, seller status | [ ] |
| RUN-10 | ADMIN | 관리자 지급 | `payout sync` 실행 | polling 후 최신 payout 상태 반영 | sync 시각, 상태 변화 | [ ] |
| RUN-11 | DRIVER | 차주 수령 확인 | `/payouts/orders/{orderId}/status` 확인 | 차주 본인 기준 payout 상태 조회 성공 | driver view status | [ ] |
| RUN-12 | QA | raw payload / 타임라인 | 단계별 raw payload 저장 | 결제/지급 핵심 응답과 식별자가 남아 있음 | 파일명 또는 로그 경로 | [ ] |
| RUN-13 | QA | 결과 저장 | 최종 결과 요약 저장 | 재현 가능한 실행 기록 완료 | 결과 문서 링크 | [ ] |
| RUN-14 | SHIPPER | 화주 결제 | 필요 시 `billing agreement deactivate` 실행 | 다음 회차를 위한 정리 완료 | cleanup 여부 | [ ] |

실행 분기 규칙:

1. `REAL_TOSS_PAYMENT_E2E`는 `RUN-05A`, `RUN-05B`를 수행하고 `RUN-05C`는 건너뛴다.
2. `MANUAL_MARK_PAID_E2E`는 `RUN-05C`를 수행하고 `RUN-05A`, `RUN-05B`는 건너뛴다.

---

## 5. 단계별 종료 판정

| 단계 | Pass 기준 | Fail 기준 |
| --- | --- | --- |
| 결제 준비 | orderId 확정, actor 전환 가능, billing context 조회 성공 | 키/토큰/주문 상태 미확정 |
| 결제 반영 | `TransportPayment=PAID`, `Settlement=READY` | prepare/confirm/mark-paid 실패 또는 내부 상태 미반영 |
| 차주 확인 | `TransportPayment=CONFIRMED`, `Order=CONFIRMED`, `Settlement=COMPLETED` | confirm 실패 또는 상태 불일치 |
| 지급 요청 | `DriverPayoutItem` 생성, `payoutRef` 또는 실패 원인 확인 가능 | item 미생성, 권한 오류, 셀러 상태 오류 |
| 지급 동기화 | `REQUESTED/COMPLETED/FAILED` 중 하나로 수렴, 최신 사유 확인 가능 | sync 이후에도 상태 미확정 |
| 차주 수령 확인 | 차주 화면에서 본인 주문 payout 상태 조회 성공 | 권한 오류, item 없음, 상태 조회 실패 |

최종 Pass 기준:

1. 주문 ID, actor, 실행 타입이 모두 기록돼 있다.
2. 최소 하나의 결제 경로가 `PAID`까지 성공했다.
3. `driver confirm`까지 성공해 `Settlement=COMPLETED`를 확인했다.
4. payout item 상태와 차주 수령 확인 결과를 남겼다.
5. raw payload 또는 타임라인 로그가 단계별로 남아 있다.

---

## 6. 실행 결과 기록 템플릿

아래 블록을 복사해서 실행마다 1회 작성한다.

```text
[QA RESULT]
run_id:
executed_at:
qa_owner:
run_type: REAL_TOSS_PAYMENT_E2E | MANUAL_MARK_PAID_E2E
server_url:
order_id:
shipper_actor:
driver_actor:
admin_actor:
agreement_id:
pg_order_id:
payment_key:
payout_item_id:
payout_ref:
final_transport_payment_status:
final_settlement_status:
final_payout_status:
driver_visible_status:
result: PASS | FAIL | BLOCKED
failure_stage:
summary:
raw_payload_location:
```
