package com.example.project.domain.order.domain.embedded;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTimeline {
    @Column(name = "ACCEPTED")
    private LocalDateTime accepted; // 수락 일시

    @Column(name = "START_TIME")
    private LocalDateTime startTime; // 출발/상차 일시

    @Column(name = "END_TIME")
    private LocalDateTime endTime; // 종료/하차 일시

    @Column(name = "COMPLETED")
    private LocalDateTime completed; // 최종 완료 일시
}