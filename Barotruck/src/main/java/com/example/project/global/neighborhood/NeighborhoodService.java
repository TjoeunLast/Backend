package com.example.project.global.neighborhood;

import java.util.List;
import java.util.stream.Collectors;

import com.example.project.global.neighborhood.dto.NeighborhoodResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NeighborhoodService {

    private final NeighborhoodRepository neighborhoodRepository;

    /**
     * 동네 이름으로 검색하여 DTO 리스트 반환
     */
    public List<NeighborhoodResponse> searchNeighborhoods(String query) {
        return neighborhoodRepository.findByDisplayNameContaining(query)
                .stream()
                .map(NeighborhoodResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NeighborhoodResponse resolveNeighborhood(String cityName, String displayName) {

        if (cityName == null || cityName.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("cityName/displayName은 필수입니다.");
        }

        Neighborhood neighborhood = neighborhoodRepository
                .findByCityNameAndDisplayName(
                        cityName.trim(),
                        displayName.trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 지역입니다: " + cityName + " " + displayName)
                );
        return NeighborhoodResponse.from(neighborhood);
    }
    public NeighborhoodResponse resolveNeighborhood(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }

        ParsedAddress area = KoreanAddressParser.parse(fullAddress);

        if (area.cityName() == null || area.displayName() == null) {
            throw new IllegalArgumentException("주소에서 지역 추출 실패: " + fullAddress);
        }

        // ⭐ 오버로딩된 다른 메서드 호출
        return resolveNeighborhood(area.cityName(), area.displayName());
    }
}