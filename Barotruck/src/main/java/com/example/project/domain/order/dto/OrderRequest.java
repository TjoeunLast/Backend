package com.example.project.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
	// --- [상차지 정보: 물건을 싣는 곳] ---
    private String startAddr;      // 상차지 전체 주소 (예: 서울특별시 강남구 테헤란로 123)
    private String startPlace;     // 상차지 특정 명칭 (예: OO물류센터 A동 3번 도크) - 기사가 위치를 정확히 찾는 데 활용
    private String startType;      // 상차 방식 (예: 당상-당일 상차, 익상-다음날 상차, 야간상차)
    private String startSchedule;  // 상차 예정 시간 (예: "2024-05-20 14:00" 또는 "오전 중")
    private Long startNbhId;       // 상차지 지역 코드 ID (행정구역 단위 관리를 위한 FK)
    private String puProvince;     // 상차지 광역 자치단체명 (예: 서울, 경기, 부산) - 지역별 오더 필터링용

    // --- [하차지 정보: 물건을 내리는 곳] ---
    private String endAddr;        // 하차지 전체 주소 (예: 경기도 용인시 처인구 ...)
    private String endPlace;       // 하차지 특정 명칭 (예: XX빌딩 후문 하역장)
    private String endType;        // 하차 방식 (예: 당착-당일 도착, 내착-내일 도착)
    private String endSchedule;    // 하차 예정 시간
    private Long endNbhId;         // 하차지 지역 코드 ID
    private String doProvince;     // 하차지 광역 자치단체명 (예: 경기, 강원, 전남)

    // --- [화물 및 작업 세부 정보] ---
    private String cargoContent;   // 화물 내용물 (예: 정밀 기계, 파레트 짐, 농산물 등)
    private String loadMethod;     // 적재 방식 (예: 독차-차 한 대 전체 사용, 혼적-다른 짐과 같이 적재)
    private String workType;       // 상하차 작업 도구 (예: 지게차, 수작업, 크레인 등)
    private BigDecimal tonnage;    // 화물 무게 단위 (예: 2.5 - 톤 단위)
    private String reqCarType;     // 요청 차량 종류 (예: 카고, 윙바디, 냉동탑차, 라보 등)
    private String reqTonnage;     // 요청 차량 톤수 (예: 1톤, 5톤, 11톤 등)
    private String driveMode;      // 운행 모드 (예: 편도, 왕복, 경유 있음)
    private Long loadWeight;       // 실제 적재 중량 (Kg 단위 등 세부 수치)

    // --- [금액 및 결제 정보] ---
    private Long basePrice;        // 기본 운송료 (거리 및 톤수 기준 표준 운임)
    private Long laborFee;         // 수작업비 (기사님이 직접 상하차를 도울 경우 발생하는 수고비)
    private Long packagingPrice;   // 포장비용 (물건 보호를 위한 래핑, 파레트 제공 등 실비)
    private Long insuranceFee;     // 적재물 보험료 (고가 화물일 경우 추가되는 보험 비용)
    private String payMethod;      // 결제 방식 (예: 신용카드, 계좌이체, 인수증/후불, 선불)

    // --- [시스템 계산 지표: 지도 API 연동 결과] ---
    private Long distance;         // 예상 주행 거리 (단위: 미터 또는 킬로미터)
    private Long duration;         // 예상 소요 시간 (단위: 초 또는 분)
}