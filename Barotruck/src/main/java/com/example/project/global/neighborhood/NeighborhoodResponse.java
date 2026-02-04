package com.example.project.global.neighborhood;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NeighborhoodResponse {
    private Long neighborhoodId;
    private String cityName;
    private String displayName;
    private String fullName; // 예: "서울특별시 종로구"

    public static NeighborhoodResponse from(Neighborhood neighborhood) {
        return NeighborhoodResponse.builder()
                .neighborhoodId(neighborhood.getNeighborhoodId())
                .cityName(neighborhood.getCityName())
                .displayName(neighborhood.getDisplayName())
                .fullName(neighborhood.getCityName() + " " + neighborhood.getDisplayName())
                .build();
    }
}
