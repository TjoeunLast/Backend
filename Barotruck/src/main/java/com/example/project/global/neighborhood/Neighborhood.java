package com.example.project.global.neighborhood;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "neighborhoods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Neighborhood {

    @Id
    @Column(name = "neighborhood_id")
    private Long neighborhoodId; // 예: 11110 (서울 종로구), 41110 (경기 수원시)

    @Column(nullable = false, length = 50)
    private String cityName;     // 광역자치단체 (예: 서울특별시, 경기도)

    @Column(nullable = false, length = 100)
    private String displayName;  // 표시용 이름 (예: 종로구, 수원시 영통구)

    @Builder
    public Neighborhood(Long neighborhoodId, String cityName, String displayName) {
        this.neighborhoodId = neighborhoodId;
        this.cityName = cityName;
        this.displayName = displayName;
    }
}
