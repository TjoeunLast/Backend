# Toss 결제 보강 10인 분업 계획

기준일: 2026-03-10

이 문서는 `docs/toss-payment-enhancement-plan.md`와 `docs/toss-payment-implementation-handoff.md`를 기반으로, 남은 작업을 10명 기준으로 분업하기 위한 실행 문서다.

현재 남은 미비점과 문서 불일치 정리:

- `docs/toss-payment-gap-review.md`

---

## 1. 공통 원칙

1. 각 담당자는 자기 범위 외 파일 수정 시 사전 공유
2. DB/환경변수 변경은 백엔드 API 작업보다 먼저 반영
3. 관리자 웹은 백엔드 Swagger 확인 후 연결
4. 런타임 검증은 test key 기준으로 먼저 수행
5. 실환불/실지급 검증은 운영 데이터와 분리

---

## 2. 담당자 배정

## 담당자 1. DB / DDL

범위:

- 신규 테이블 생성
- 기존 테이블 컬럼 추가
- 인덱스/유니크 제약 확인

대상:

- `SHIPPER_BILLING_AGREEMENTS`
- `FEE_AUTO_CHARGE_ATTEMPTS`
- `PAYMENT_GATEWAY_TRANSACTIONS` 추가 컬럼

완료 기준:

- 개발 DB 반영 완료
- 애플리케이션 기동 및 `compileJava` 이후 서버 실행 확인
- DDL 스크립트 문서화

의존성:

- 가장 먼저 시작

---

## 담당자 2. 사용자 결제 API

범위:

- 화주 billing agreement API 연결

대상 파일:

- [PaymentController.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/controller/PaymentController.java)
- [ShipperBillingAgreementService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/ShipperBillingAgreementService.java)

구현 항목:

- billing context 조회
- billing key 발급
- 내 billing agreement 조회
- billing agreement 해지

완료 기준:

- Swagger 노출
- SHIPPER 권한으로 정상 호출
- 401/403/400 분기 확인

의존성:

- 담당자 1 이후

---

## 담당자 3. 관리자 결제 운영 API

범위:

- Toss 실조회 / 실취소 API 노출

대상 파일:

- [AdminPaymentController.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java)
- [TossPaymentOpsService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/TossPaymentOpsService.java)

구현 항목:

- `GET /api/admin/payment/toss/payments/{paymentKey}`
- `GET /api/admin/payment/toss/orders/{orderId}/lookup`
- `POST /api/admin/payment/orders/{orderId}/cancel`

완료 기준:

- 관리자 권한으로 호출 가능
- cancel 실패/부분취소 차단 정책 확인
- 내부 상태와 PG 호출 분리 유지

의존성:

- 담당자 1 이후

---

## 담당자 4. 관리자 운영 조회/DTO

범위:

- 관리자 화면이 쓸 조회 DTO 정리

대상 파일:

- [AdminPaymentStatusQueryService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/query/AdminPaymentStatusQueryService.java)
- `src/main/java/com/example/project/domain/payment/dto/paymentResponse/*`

구현 항목:

- billing agreement 상태 조회
- auto-charge attempt 목록 조회
- payout webhook 반영 상태 조회
- lookup/comparison 응답 다듬기

완료 기준:

- 관리자 웹에서 추가 가공 없이 바로 쓸 수 있는 응답 shape 제공

의존성:

- 담당자 1 이후

---

## 담당자 5. webhook / 상태동기화

범위:

- payout/seller webhook 안정화
- cancel webhook 반영 안정화

대상 파일:

- [TossPaymentService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/global/toss/service/TossPaymentService.java)
- [TossPayoutWebhookService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/TossPayoutWebhookService.java)
- [PaymentLifecycleService.java](/C:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java)

구현 항목:

- sample payload 기준 이벤트 분기 검증
- cancel 후 내부 상태 수렴 검증
- payout webhook과 polling 충돌 규칙 정리

완료 기준:

- 샘플 webhook 재전송에도 상태 꼬임 없음
- 멱등성 유지

의존성:

- 담당자 1 이후

---

## 담당자 6. 설정 / 배포 / 환경변수

범위:

- Toss 결제/빌링/지급 설정 정리

대상 파일:

- [application.properties](/C:/ytheory/springPro/Backend/Barotruck/src/main/resources/application.properties)
- 실행 환경 문서

구현 항목:

- billing secret key
- billing redirect url
- fee auto charge mock flag
- test/live key 분리

완료 기준:

- 로컬/개발/운영에 필요한 env 목록 정리
- 누락 시 실패 방식 명확화

의존성:

- 담당자 1과 병렬 가능

---

## 담당자 7. 관리자 웹 화주 정산 상세

범위:

- PG 실조회 패널

대상 파일:

- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts`

구현 항목:

- 내부 결제 상태 표시
- Toss 실조회 결과 표시
- mismatch 배지

완료 기준:

- 주문 상세에서 내부/PG 상태 비교 가능

의존성:

- 담당자 3, 4 이후

---

## 담당자 8. 관리자 웹 환불/취소 UX

범위:

- 실취소/환불 UI

대상 파일:

- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`
- 필요 시 새 modal 컴포넌트

구현 항목:

- 취소 사유 입력 모달
- 취소 버튼
- 성공/실패 메시지

완료 기준:

- 관리자가 UI에서 취소 요청 가능
- 실패 시 명확한 원인 확인 가능

의존성:

- 담당자 3 이후

---

## 담당자 9. 관리자 웹 차주 지급 운영

범위:

- payout 상태 운영 패널

대상 파일:

- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx`
- 필요 시 상세 컴포넌트

구현 항목:

- payout item 상태
- seller 상태
- 최근 webhook 반영 시각
- polling/webhook 충돌 경고

완료 기준:

- 차주 지급 장애를 관리자 화면에서 판별 가능

의존성:

- 담당자 4, 5 이후

---

## 담당자 10. 화주 앱 billing / QA

범위:

- 화주 앱 billing 등록 흐름
- 전체 통합 테스트

대상 파일:

- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app`

구현 항목:

- billing key 등록 화면
- 등록 후 agreement 조회
- 해지 흐름
- 결제/취소/빌링/지급 테스트 시나리오 작성

완료 기준:

- 앱에서 billing 등록/조회/해지 가능
- 테스트 체크리스트 문서화

의존성:

- 담당자 2, 6 이후

---

## 3. 추천 순서

1. 담당자 1
2. 담당자 2, 3, 4, 6 병렬
3. 담당자 5
4. 담당자 7, 8, 9 병렬
5. 담당자 10

---

## 4. 검수 포인트

각 담당자는 아래를 남겨야 한다.

1. 수정 파일 목록
2. Swagger 또는 화면 캡처
3. 실패 케이스 검증 결과
4. 테스트 방법
5. 다음 담당자가 이어받을 주의사항

---

## 5. 완료 체크

- [ ] DB 반영 완료
- [ ] billing API 완료
- [ ] Toss lookup/cancel API 완료
- [ ] 관리자 조회 DTO 완료
- [ ] webhook 동기화 완료
- [ ] 환경변수/설정 정리 완료
- [ ] 관리자 화주 정산 상세 완료
- [ ] 관리자 환불/취소 UX 완료
- [ ] 관리자 차주 지급 운영 화면 완료
- [ ] 화주 앱 billing + QA 완료

---

## 6. 연관 문서

- [toss-payment-enhancement-plan.md](/C:/ytheory/springPro/Backend/Barotruck/docs/toss-payment-enhancement-plan.md)
- [toss-payment-implementation-handoff.md](/C:/ytheory/springPro/Backend/Barotruck/docs/toss-payment-implementation-handoff.md)
- [toss-payment-project-summary.md](/C:/ytheory/springPro/Backend/Barotruck/docs/toss-payment-project-summary.md)
