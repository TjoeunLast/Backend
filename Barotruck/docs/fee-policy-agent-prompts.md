# 양면 수수료 정책 멀티 에이전트 분업 프롬프트

기준일: 2026-03-11

이 문서는 [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)를 실제 구현으로 옮기기 위해 역할별 에이전트에게 바로 전달할 수 있는 프롬프트 모음이다.

이번 개선안의 핵심 정책은 아래다.

- 화주 side 수수료율과 차주 side 수수료율은 각각 `1.5% ~ 2.5%` 범위에서 레벨별로 적용한다.
- 결과적으로 플랫폼 총 수수료율은 거래금액 기준 `약 3% ~ 5%`가 되게 설계한다.
- 화주는 `첫 결제 프로모션`
- 차주는 `첫 운송 프로모션`
- `Toss 수수료`는 전체 거래금액 기준으로 `가장 마지막`에 차감한다.
- 프런트 하드코드 계산은 제거하고, 백엔드 preview/snapshot을 source of truth로 삼는다.

---

## 작업 루트

백엔드 루트:

- `C:\ytheory\springPro\Backend\Barotruck`

모바일 앱 루트:

- `C:\ytheory\Backend\Barotruck\FrontNew\barotruck-app`

관리자 웹 루트:

- `C:\ytheory\Backend\Barotruck\Admin_FrontEnd\barotruck_admin_web`

---

## 권장 진행 순서

1. `에이전트 0`이 계약과 필드 이름을 먼저 잠근다.
2. `에이전트 1~5`가 백엔드 코어를 병렬 구현한다.
3. `에이전트 6~8`이 모바일/관리자 화면을 붙인다.
4. `에이전트 9~10`이 데이터 이행, QA, 관측, 롤아웃을 닫는다.

---

## 공통 전제

- 기준 문서는 [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)다.
- 정책 설명은 `각 side 1.5% ~ 2.5%, 총합 약 3% ~ 5%` 형식으로 통일한다.
- 화주 화면은 `shipper side fee`만, 차주 화면은 `driver side fee`만 1차적으로 보여준다.
- `Toss 수수료`는 내부 운영/관리 화면과 최종 정산 스냅샷에는 보여주되, 일반 사용자 화면에서는 설명 수준으로만 노출한다.
- 결제/정산 완료 후에는 당시 정책을 그대로 재현할 수 있게 `snapshot 저장`을 우선한다.
- 기존 `FeePolicyService`, `PaymentLifecycleService`, 관리자 수수료 설정 화면이 이미 있으므로 완전 신규 구현보다 확장을 우선한다.

---

## 에이전트 0. 계약 및 경계 코디네이터

### 목적

양면 수수료 정책의 필드, 계산 순서, API shape, 스냅샷 저장 범위를 먼저 고정한다.

### 반드시 읽을 파일

- [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)
- [FeePolicyService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/FeePolicyService.java)
- [PaymentLifecycleService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java)
- [feePolicy.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts)
- [payment_admin_api.ts](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts)

### 주요 산출물

- `docs/fee-policy-contracts.md`
- `docs/fee-policy-snapshot-fields.md`
- `docs/fee-policy-agent-ownership.md`

### 꼭 결정해야 할 것

- `shipper side / driver side / toss` 필드 이름 최종안
- preview API request/response 최종 shape
- 결제 확정 후 저장할 snapshot 필드 목록
- 프로모션 eligibility 판정 기준
- 관리자 화면에서 수정 가능한 정책 범위와 검증 규칙
- 손익 음수 주문 표시 기준

### 금지사항

- 실제 비즈니스 로직 구현까지 깊게 들어가지 말 것
- 프런트와 백엔드가 각자 다른 용어를 쓰게 두지 말 것

### 완료 조건

- 다른 에이전트가 이 문서만 보고 자신의 범위를 바로 착수할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책 프로젝트의 계약 및 경계 코디네이터다.

목표:
- `docs/shipper-fee-policy-alignment.md`를 구현 계약 문서로 구체화하라.
- 백엔드, 모바일 앱, 관리자 웹이 같은 필드명과 계산 순서를 쓰게 고정하라.

반드시 읽을 파일:
- `docs/shipper-fee-policy-alignment.md`
- `src/main/java/com/example/project/domain/payment/service/core/FeePolicyService.java`
- `src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts`

산출물:
- `docs/fee-policy-contracts.md`
- `docs/fee-policy-snapshot-fields.md`
- `docs/fee-policy-agent-ownership.md`

반드시 포함:
- side fee 필드명 표준
- preview API 계약
- snapshot 저장 필드
- 프로모션 eligibility 기준
- 손익 음수 주문 표시 기준
- 에이전트별 파일 소유권

중요:
- `각 side 1.5% ~ 2.5%, 총합 약 3% ~ 5%` 형식으로 설명하라.
- `Toss 수수료는 마지막 차감` 규칙을 계약에 못 박아라.
```

---

## 에이전트 1. 정책 스키마 및 관리자 설정 API 담당

### 목적

기존 단면 수수료 정책 테이블과 DTO를 양면 정책 구조로 확장한다.

### 반드시 읽을 파일

- [FeePolicyConfig.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/domain/FeePolicyConfig.java)
- [FeePolicyConfigRepository.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/repository/FeePolicyConfigRepository.java)
- [UpdateFeePolicyRequest.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentRequest/UpdateFeePolicyRequest.java)
- [FeePolicyResponse.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentResponse/FeePolicyResponse.java)
- [LevelFeePolicyResponse.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentResponse/LevelFeePolicyResponse.java)
- [AdminPaymentController.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java)
- [page.tsx](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/settings/page.tsx)

### 주요 산출물

- 정책 엔티티/DTO/컨트롤러 변경
- 필요한 DDL 또는 migration
- 관리자 조회/수정 API 호환성 정리

### 꼭 고려할 것

- `shipperSide`와 `driverSide`를 분리
- `shipperFirstPaymentPromo`, `driverFirstTransportPromo`를 분리
- `tossRate`를 정책 응답에 포함
- 기존 관리자 화면이 깨지지 않게 전환 전략 마련

### 금지사항

- preview 계산 엔진까지 같이 떠안지 말 것
- 결제 확정 후 snapshot 저장 책임을 가져가지 말 것

### 완료 조건

- 관리자 API만 호출해도 현재 정책의 모든 side rate와 promo/toss 설정을 확인하고 수정할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 정책 스키마 및 관리자 설정 API 담당이다.

목표:
- 기존 단면 `FeePolicyConfig` 구조를 양면 정책 구조로 확장하라.
- 관리자 API가 shipper side, driver side, promo, toss rate를 모두 다룰 수 있게 하라.

반드시 읽을 파일:
- `src/main/java/com/example/project/domain/payment/domain/FeePolicyConfig.java`
- `src/main/java/com/example/project/domain/payment/repository/FeePolicyConfigRepository.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentRequest/UpdateFeePolicyRequest.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentResponse/FeePolicyResponse.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentResponse/LevelFeePolicyResponse.java`
- `src/main/java/com/example/project/domain/payment/controller/AdminPaymentController.java`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/settings/page.tsx`

산출물:
- 엔티티/DTO/컨트롤러 수정
- migration 또는 DDL
- 관리자 API 전환 전략

반드시 포함:
- shipper side level rates
- driver side level rates
- shipper first payment promo
- driver first transport promo
- toss rate
- 입력값 검증 규칙

중요:
- 기존 단면 필드는 무턱대고 제거하지 말고 전환 경로를 만들어라.
- `각 side 1.5% ~ 2.5%` 범위를 기본 정책으로 반영하라.
```

---

## 에이전트 2. 수수료 계산 엔진 담당

### 목적

양면 수수료 계산을 단일 백엔드 엔진으로 중앙화한다.

### 반드시 읽을 파일

- [FeePolicyService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/FeePolicyService.java)
- [PaymentLifecycleService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java)
- [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)
- [feePolicy.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts)
- [feePolicy.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/utils/feePolicy.ts)

### 주요 산출물

- 중앙 계산 서비스
- preview용 breakdown DTO
- rounding/min/max 정책 구현

### 꼭 고려할 것

- shipper fee와 driver fee 별도 계산
- promo는 해당 side에만 적용
- toss는 `shipperChargeAmount` 기준 마지막 계산
- `platformGrossRevenue`와 `platformNetRevenue` 모두 반환
- 프런트 계산과 100% 동일한 수치 재현

### 금지사항

- 컨트롤러/화면 로직까지 같이 들고 가지 말 것
- 기존 수수료 계산이 여러 군데 흩어져 남지 않게 둘 것

### 완료 조건

- 한 서비스 호출만으로 preview와 snapshot 저장에 필요한 모든 숫자를 얻을 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 수수료 계산 엔진 담당이다.

목표:
- shipper side, driver side, toss fee를 하나의 중앙 계산 엔진으로 통합하라.
- preview와 실제 결제 snapshot이 같은 계산 결과를 쓰게 하라.

반드시 읽을 파일:
- `src/main/java/com/example/project/domain/payment/service/core/FeePolicyService.java`
- `src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java`
- `docs/shipper-fee-policy-alignment.md`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/utils/feePolicy.ts`

산출물:
- 중앙 계산 서비스
- breakdown DTO
- 단위 테스트

반드시 포함:
- shipperFeeRate / shipperFeeAmount
- driverFeeRate / driverFeeAmount
- shipperPromoApplied / driverPromoApplied
- shipperChargeAmount / driverPayoutAmount
- tossFeeRate / tossFeeAmount
- platformGrossRevenue / platformNetRevenue

중요:
- 계산 순서는 문서와 동일해야 한다.
- `Toss 수수료는 마지막 차감` 규칙을 절대 바꾸지 마라.
- 프런트 하드코드를 용인하지 말고 백엔드를 source of truth로 만들어라.
```

---

## 에이전트 3. 프로모션 eligibility 및 사용자 레벨 연동 담당

### 목적

화주 첫 결제, 차주 첫 운송 프로모션 적용 여부를 정확히 판정한다.

### 반드시 읽을 파일

- [UsersService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/member/service/UsersService.java)
- [OrderService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/order/service/OrderService.java)
- [TransportPaymentRepository.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/repository/TransportPaymentRepository.java)
- [DriverPayoutItemRepository.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/repository/DriverPayoutItemRepository.java)
- [UserPort.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/port/UserPort.java)
- [OrderPort.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/port/OrderPort.java)

### 주요 산출물

- 프로모션 eligibility 서비스
- 필요한 repository query 추가
- 레벨/프로모션 판정 문서

### 꼭 고려할 것

- `첫 결제`의 기준 상태
- `첫 운송`의 기준 상태
- 취소/분쟁/관리자 강제처리 건의 포함 여부
- 멱등성
- 동시성으로 인한 이중 프로모션 방지

### 금지사항

- 금액 계산 공식을 여기서 재구현하지 말 것
- UI 노출까지 같이 처리하지 말 것

### 완료 조건

- 어떤 주문이든 프로모션 적용 여부를 재현 가능하게 판정할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 프로모션 eligibility 및 사용자 레벨 연동 담당이다.

목표:
- 화주의 첫 결제 프로모션과 차주의 첫 운송 프로모션을 정확하게 판정하라.
- 결제 취소, 분쟁, 관리자 처리, 동시성 상황에서도 중복 적용이 없게 하라.

반드시 읽을 파일:
- `src/main/java/com/example/project/member/service/UsersService.java`
- `src/main/java/com/example/project/domain/order/service/OrderService.java`
- `src/main/java/com/example/project/domain/payment/repository/TransportPaymentRepository.java`
- `src/main/java/com/example/project/domain/payment/repository/DriverPayoutItemRepository.java`
- `src/main/java/com/example/project/domain/payment/port/UserPort.java`
- `src/main/java/com/example/project/domain/payment/port/OrderPort.java`

산출물:
- eligibility service
- 관련 query 메서드
- 판정 기준 문서

반드시 포함:
- 첫 결제 기준 상태 정의
- 첫 운송 기준 상태 정의
- 취소/분쟁 건 처리 원칙
- 멱등성 및 동시성 방어

중요:
- 프로모션 로직은 계산 엔진과 분리하라.
- side별 eligibility만 정확히 판정하고 계산 자체는 중앙 엔진에 넘겨라.
```

---

## 에이전트 4. 프리뷰 API 및 결제 진입 DTO 담당

### 목적

화주 오더 생성과 결제 직전에 백엔드 preview를 조회할 수 있게 공개 API를 만든다.

### 반드시 읽을 파일

- [PaymentController.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/controller/PaymentController.java)
- [TransportPaymentService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/TransportPaymentService.java)
- [MarkPaidRequest.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentRequest/MarkPaidRequest.java)
- [TossPrepareRequest.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentRequest/TossPrepareRequest.java)
- [CreateOrderStep1Screen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep1Screen.tsx)
- [CreateOrderStep2CargoScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep2CargoScreen.tsx)

### 주요 산출물

- `POST /api/v1/payments/fee-preview`
- request/response DTO
- 인증/권한/에러 응답 정의

### 꼭 고려할 것

- shipper만 호출 가능한지, admin도 호출 가능한지 정책 확정
- driver가 아직 미배정인 오더 생성 단계 처리
- 특정 payment provider별 preview 차이
- 프리뷰와 실제 결제값 차이가 나는 경우의 원인 표시

### 금지사항

- 결제 완료 후 snapshot 저장 로직까지 같이 맡지 말 것
- 프런트에서 다시 계산하라고 넘기지 말 것

### 완료 조건

- 화주 앱이 이 API만 호출하면 오더 생성 화면과 결제 화면의 금액 표시를 모두 구성할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 프리뷰 API 및 결제 진입 DTO 담당이다.

목표:
- 화주 오더 생성과 결제 직전에 호출할 `fee-preview` API를 만들고 안정화하라.
- 프런트가 더 이상 하드코드 계산을 하지 않게 하라.

반드시 읽을 파일:
- `src/main/java/com/example/project/domain/payment/controller/PaymentController.java`
- `src/main/java/com/example/project/domain/payment/service/core/TransportPaymentService.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentRequest/MarkPaidRequest.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentRequest/TossPrepareRequest.java`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep1Screen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep2CargoScreen.tsx`

산출물:
- `POST /api/v1/payments/fee-preview`
- request/response DTO
- 예외 처리 및 테스트

반드시 포함:
- baseAmount
- shipper side breakdown
- driver side breakdown
- toss breakdown
- gross/net revenue
- promo 적용 여부

중요:
- 프런트가 다시 계산하지 않도록 충분한 응답 필드를 내려라.
- 미배정 단계에서도 preview 가능한 규칙을 명확히 하라.
```

---

## 에이전트 5. 결제 라이프사이클 및 스냅샷 영속화 담당

### 목적

결제 완료/확정 시 양면 수수료 스냅샷을 영속화하고, 이후 정산/지급이 그 스냅샷만 바라보게 만든다.

### 반드시 읽을 파일

- [PaymentLifecycleService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java)
- [TransportPayment.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/domain/TransportPayment.java)
- [Settlement.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/settlement/domain/Settlement.java)
- [SettlementService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/settlement/service/SettlementService.java)
- [SettlementResponse.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/settlement/dto/SettlementResponse.java)
- [TransportPaymentResponse.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/dto/paymentResponse/TransportPaymentResponse.java)

### 주요 산출물

- 결제/정산 엔티티 컬럼 추가
- snapshot 저장 로직
- 조회 DTO 반영

### 꼭 고려할 것

- 결제 시점과 확정 시점 중 어디에 무엇을 저장할지
- 수수료 정책 변경 후에도 과거 주문 재현 가능해야 함
- 수동 결제, Toss 결제, 자동 확정 모두 같은 필드 체계 사용
- 정산 화면이 필요한 데이터가 중복 계산 없이 내려와야 함

### 금지사항

- 수수료 계산 공식을 엔티티에 흩뿌리지 말 것
- snapshot 없이 실시간 재계산에 의존하지 말 것

### 완료 조건

- 과거 결제 건도 당시 snapshot만으로 사용자 화면과 관리자 화면을 재현할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 결제 라이프사이클 및 스냅샷 영속화 담당이다.

목표:
- 결제 완료/확정 시 양면 수수료 스냅샷을 저장하라.
- 이후 정산/지급/관리자 조회는 실시간 재계산이 아니라 snapshot을 기준으로 동작하게 하라.

반드시 읽을 파일:
- `src/main/java/com/example/project/domain/payment/service/core/PaymentLifecycleService.java`
- `src/main/java/com/example/project/domain/payment/domain/TransportPayment.java`
- `src/main/java/com/example/project/domain/settlement/domain/Settlement.java`
- `src/main/java/com/example/project/domain/settlement/service/SettlementService.java`
- `src/main/java/com/example/project/domain/settlement/dto/SettlementResponse.java`
- `src/main/java/com/example/project/domain/payment/dto/paymentResponse/TransportPaymentResponse.java`

산출물:
- 엔티티 컬럼 추가
- snapshot 저장 로직
- 조회 DTO 확장
- 테스트

반드시 포함:
- shipper fee snapshot
- driver fee snapshot
- promo applied flags
- toss fee snapshot
- gross/net revenue snapshot
- policy version 또는 policy timestamp

중요:
- 과거 정책 변경이 과거 주문에 영향을 주지 않게 하라.
- 수동 결제와 Toss 결제가 같은 스냅샷 모델을 쓰게 하라.
```

---

## 에이전트 6. 정산 원장, 청구서, 지급 연동 담당

### 목적

양면 스냅샷이 화주 청구서와 차주 지급 원장에 올바르게 반영되게 한다.

### 반드시 읽을 파일

- [FeeInvoiceService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceService.java)
- [FeeInvoiceBatchService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceBatchService.java)
- [DriverPayoutService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/core/DriverPayoutService.java)
- [FeeInvoice.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/domain/FeeInvoice.java)
- [FeeInvoiceItem.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/domain/FeeInvoiceItem.java)
- [DriverPayoutItem.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/domain/DriverPayoutItem.java)
- [AdminPaymentStatusQueryService.java](c:/ytheory/springPro/Backend/Barotruck/src/main/java/com/example/project/domain/payment/service/query/AdminPaymentStatusQueryService.java)

### 주요 산출물

- 차주 지급 금액과 화주 청구 금액의 snapshot 기반 반영
- 원장/조회 DTO 수정
- 운영 조회 일관성 정리

### 꼭 고려할 것

- shipper 청구서에는 shipper side fee만
- driver payout에는 driver side fee만
- 관리자 조회에는 양쪽 side와 toss, gross/net 모두
- 재시도/취소/분쟁 건의 금액 정합성

### 금지사항

- 사용자 화면을 먼저 맞추기 위해 원장 수치가 틀어지게 두지 말 것

### 완료 조건

- 청구서, 지급 원장, 관리자 운영 조회가 모두 같은 snapshot 숫자를 바라봐야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 정산 원장, 청구서, 지급 연동 담당이다.

목표:
- 결제 snapshot이 화주 청구서와 차주 지급 원장에 정확히 반영되게 하라.
- 청구/지급/관리자 조회 숫자가 서로 다르게 보이는 상황을 없애라.

반드시 읽을 파일:
- `src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceService.java`
- `src/main/java/com/example/project/domain/payment/service/core/FeeInvoiceBatchService.java`
- `src/main/java/com/example/project/domain/payment/service/core/DriverPayoutService.java`
- `src/main/java/com/example/project/domain/payment/domain/FeeInvoice.java`
- `src/main/java/com/example/project/domain/payment/domain/FeeInvoiceItem.java`
- `src/main/java/com/example/project/domain/payment/domain/DriverPayoutItem.java`
- `src/main/java/com/example/project/domain/payment/service/query/AdminPaymentStatusQueryService.java`

산출물:
- 원장/청구/지급 로직 수정
- 조회 DTO 수정
- 정합성 테스트

반드시 포함:
- shipper charge amount 반영
- driver payout amount 반영
- 양 side fee 분리
- toss/gross/net 관리자 조회 반영
- 취소/분쟁 건 처리 규칙

중요:
- snapshot 숫자를 재계산하지 말고 원장 기준으로 흘려라.
- 사용자 화면보다 원장 정합성을 우선하라.
```

---

## 에이전트 7. 화주 앱 오더 생성 및 결제 UX 담당

### 목적

화주 앱이 양면 수수료 정책에 맞는 preview와 결제 설명을 정확히 보여주게 한다.

### 반드시 읽을 파일

- [CreateOrderStep1Screen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep1Screen.tsx)
- [CreateOrderStep2CargoScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep2CargoScreen.tsx)
- [ShipperSettlementScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/settlement/ui/ShipperSettlementScreen.tsx)
- [paymentService.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/api/paymentService.ts)
- [feePolicy.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts)
- [feePolicy.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/utils/feePolicy.ts)

### 주요 산출물

- preview API 연동
- 하드코드 제거
- 화주용 수수료 설명 UI 정리

### 꼭 고려할 것

- 화주 화면에는 shipper side fee만 핵심적으로 노출
- `차주 side fee는 별도 정산에서 차감됩니다.` 문구 유지
- `Toss 수수료는 플랫폼 최종 정산 단계에서 마지막에 반영됩니다.` 안내 반영
- preview 로딩 실패/지연 시 fallback UX

### 금지사항

- 프런트에서 임의 계산으로 숫자를 덮어쓰지 말 것
- 차주 side 금액을 화주에게 과하게 노출하지 말 것

### 완료 조건

- 오더 생성 Step1, Step2, 결제 이후 정산 화면까지 화주 입장에서 숫자/문구가 모두 일관돼야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 화주 앱 오더 생성 및 결제 UX 담당이다.

목표:
- 오더 생성과 결제 화면에서 shipper side fee를 preview API 기준으로 정확히 보여주고, 기존 하드코드를 제거하라.

반드시 읽을 파일:
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep1Screen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/create-order/ui/CreateOrderStep2CargoScreen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/shipper/settlement/ui/ShipperSettlementScreen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/api/paymentService.ts`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/feePolicy.ts`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/utils/feePolicy.ts`

산출물:
- preview 연동 화면 수정
- 로딩/오류/fallback UX
- 문구 정리

반드시 포함:
- 기본 운임
- shipper side fee
- shipper promo 적용 여부
- 최종 화주 청구 금액
- 차주 side fee 안내 문구
- toss 마지막 차감 안내 문구

중요:
- 프런트 자체 계산은 최소화하고 preview 응답을 우선하라.
- 화주가 이해하기 쉬운 정보만 전면에 보여라.
```

---

## 에이전트 8. 차주 앱 정산 UX 담당

### 목적

차주가 실제 수령 예정 금액과 차주 side fee를 명확히 이해할 수 있게 정산 화면을 정리한다.

### 반드시 읽을 파일

- [DriverSettlementScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/driver/settlement/ui/DriverSettlementScreen.tsx)
- [CommonSettlementScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/settlement/ui/CommonSettlementScreen.tsx)
- [Settlement.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/Settlement.ts)
- [paymentService.ts](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/api/paymentService.ts)
- [DriverSettlementAccountScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/settings/ui/DriverSettlementAccountScreen.tsx)

### 주요 산출물

- 차주 정산 리스트/상세 UX 정리
- driver side fee/promo/payout 표시
- 상태 안내 문구 정리

### 꼭 고려할 것

- `기본 운임`, `차주 side fee`, `프로모션`, `최종 수령 예정 금액` 분리
- `결제 확인 대기` 상태와 수수료 설명 충돌 없게 정리
- 완료/미확인/대기 상태별 카드 정보 일관성 유지

### 금지사항

- Toss 수수료를 차주 화면 핵심 정보처럼 전면에 노출하지 말 것
- shipper side 수수료를 차주 UX 중심 정보로 섞지 말 것

### 완료 조건

- 차주가 “왜 이 금액을 받는지”를 화면만 보고 이해할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 차주 앱 정산 UX 담당이다.

목표:
- 차주 정산 화면에서 driver side fee와 최종 수령 예정 금액을 명확히 보여주고, 상태 흐름과 설명을 정리하라.

반드시 읽을 파일:
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/driver/settlement/ui/DriverSettlementScreen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/settlement/ui/CommonSettlementScreen.tsx`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/models/Settlement.ts`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/shared/api/paymentService.ts`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/settings/ui/DriverSettlementAccountScreen.tsx`

산출물:
- 정산 리스트/상세 화면 수정
- 상태별 문구 정리
- 타입 수정

반드시 포함:
- 기본 운임
- driver side fee
- driver promo 적용 여부
- 최종 수령 예정 금액
- 결제 확인 대기 / 완료 상태 정리

중요:
- 차주가 받는 돈을 중심으로 UX를 설계하라.
- shipper side 정보와 toss 정보는 관리자 수준으로 과노출하지 마라.
```

---

## 에이전트 9. 관리자 웹 정책 운영 및 시뮬레이터 담당

### 목적

운영자가 side별 정책과 프로모션을 수정하고, 주문 단위 손익을 즉시 시뮬레이션할 수 있게 한다.

### 반드시 읽을 파일

- [page.tsx](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/settings/page.tsx)
- [payment_admin_api.ts](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts)
- [page.tsx](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/page.tsx)
- [page.tsx](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx)
- [page.tsx](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx)
- [settlement_api.ts](c:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/settlement_api.ts)

### 주요 산출물

- 정책 편집 UI
- fee preview 시뮬레이터
- 손익 음수 주문 배지/필터

### 꼭 고려할 것

- shipper side와 driver side를 분리 편집
- promo rate 또는 promo mode를 별도 설정
- toss rate와 gross/net을 같이 표시
- 운영자가 적자 주문을 빠르게 찾을 수 있는 필터 제공

### 금지사항

- 관리자 화면에서 프런트 자체 계산으로 숫자를 조작하지 말 것
- 모바일 UX를 그대로 복사하지 말 것

### 완료 조건

- 운영자가 관리자 화면만으로 정책을 수정하고, 특정 금액/레벨 조합의 손익을 바로 시뮬레이션할 수 있어야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 관리자 웹 정책 운영 및 시뮬레이터 담당이다.

목표:
- 운영자가 shipper side, driver side, promo, toss rate를 수정하고 손익을 즉시 시뮬레이션할 수 있는 관리자 UI를 만들라.

반드시 읽을 파일:
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/settings/page.tsx`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/payment_admin_api.ts`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/page.tsx`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/driver/page.tsx`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/global/billing/settlement/shipper/[id]/page.tsx`
- `C:/ytheory/Backend/Barotruck/Admin_FrontEnd/barotruck_admin_web/app/features/shared/api/settlement_api.ts`

산출물:
- 정책 편집 UI
- 수수료 시뮬레이터
- 적자 주문 가시화

반드시 포함:
- shipper side rates
- driver side rates
- promo 설정
- toss rate 표시
- gross/net revenue 표시
- negative margin badge/filter

중요:
- 관리자 화면은 운영성과 설명 가능성이 중요하다.
- 숫자는 반드시 백엔드 응답을 기준으로 보여라.
```

---

## 에이전트 10. 데이터 이행, QA, 관측성 담당

### 목적

기존 주문/정산 데이터와 새 정책 구조를 안전하게 연결하고, 롤아웃과 검증 체계를 마무리한다.

### 반드시 읽을 파일

- [shipper-fee-policy-alignment.md](c:/ytheory/springPro/Backend/Barotruck/docs/shipper-fee-policy-alignment.md)
- [payment-postman-test-cases.md](c:/ytheory/springPro/Backend/Barotruck/docs/payment-postman-test-cases.md)
- [payment-test-app-qa-checklist.md](c:/ytheory/springPro/Backend/Barotruck/docs/payment-test-app-qa-checklist.md)
- [payment-env-deployment-guide.md](c:/ytheory/springPro/Backend/Barotruck/docs/payment-env-deployment-guide.md)
- [PaymentE2ELabScreen.tsx](c:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx)

### 주요 산출물

- backfill 전략 문서
- QA 시나리오
- 알람/로그/대시보드 요구사항
- 롤아웃 체크리스트

### 꼭 고려할 것

- 기존 단면 정책 주문의 해석 기준
- snapshot 미보유 과거 데이터 처리 방식
- first promo 중복 지급 검증
- negative margin Toss 주문 모니터링
- feature flag 또는 단계별 배포 전략

### 금지사항

- 데이터가 불완전한 과거 주문을 새 정책으로 덮어써서 위조하지 말 것
- QA 문서를 happy path만 쓰지 말 것

### 완료 조건

- 운영 배포 전 어떤 데이터를 옮기고 무엇을 검증해야 하는지 한 문서로 실행 가능해야 한다.

### 복사용 프롬프트

```md
너는 Barotruck 양면 수수료 정책의 데이터 이행, QA, 관측성 담당이다.

목표:
- 기존 데이터와 새 정책 구조의 전환 계획을 세우고, QA와 운영 관측 체계를 완성하라.

반드시 읽을 파일:
- `docs/shipper-fee-policy-alignment.md`
- `docs/payment-postman-test-cases.md`
- `docs/payment-test-app-qa-checklist.md`
- `docs/payment-env-deployment-guide.md`
- `C:/ytheory/Backend/Barotruck/FrontNew/barotruck-app/src/features/common/tools/payment-e2e-lab/ui/PaymentE2ELabScreen.tsx`

산출물:
- backfill/migration 전략 문서
- QA 시나리오
- 모니터링/알람 체크리스트
- 롤아웃 체크리스트

반드시 포함:
- 기존 주문 해석 규칙
- snapshot 미보유 데이터 처리 원칙
- promo 중복 검증 케이스
- negative margin Toss 주문 모니터링
- 단계별 배포 전략

중요:
- 운영 데이터 위조나 임의 보정은 금지다.
- happy path, 취소, 분쟁, 실패, 재시도까지 모두 검증하라.
```

---

## 최종 메모

이번 분업의 중심은 `양면 수수료 정책을 한 번 계산하고, 그 결과를 preview와 snapshot으로 끝까지 밀어붙이는 것`이다.

정리하면:

- 에이전트 0은 계약을 잠근다.
- 에이전트 1~6은 백엔드에서 정책, 계산, eligibility, preview, snapshot, 원장을 닫는다.
- 에이전트 7~9는 사용자/관리자 화면을 붙인다.
- 에이전트 10은 데이터 이행, QA, 운영 관측을 닫는다.

이 문서 기준으로 병렬 작업을 걸면, 정책 문구와 계산식이 흩어지는 문제를 가장 크게 줄일 수 있다.
