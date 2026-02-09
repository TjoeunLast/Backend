package com.example.project.domain.order.dto.orderRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRequest {
    // 기준 시간: “출발(상차) 예정 시각” 기준으로 주/야간 판단
    private LocalDateTime pickupAt;

    // 거리: meter 기준 권장 (지도 API가 보통 meters)
    private Long distanceMeters;

    // 공휴일 여부: 클라이언트에서 보내거나, 서버에서 나중에 Holiday 테이블/외부 API로 판정하도록 확장 가능
    private Boolean isHoliday;
}
