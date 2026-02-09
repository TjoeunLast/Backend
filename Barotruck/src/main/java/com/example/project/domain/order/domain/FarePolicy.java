package com.example.project.domain.order.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fare_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FarePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayType dayType;   // WEEKDAY / WEEKEND / HOLIDAY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeType timeType; // DAY / NIGHT

    // 기본거리(km)까지는 기본요금만
    @Column(nullable = false)
    private Integer baseDistanceKm;

    // 기본요금
    @Column(nullable = false)
    private Long baseFare;

    // 기본거리 초과 시 km당 추가요금
    @Column(nullable = false)
    private Long perKmFare;

    // 최저요금(선택)
    @Column(nullable = true)
    private Long minimumFare;

    public enum DayType {
        WEEKDAY, WEEKEND, HOLIDAY
    }

    public enum TimeType {
        DAY, NIGHT
    }
}