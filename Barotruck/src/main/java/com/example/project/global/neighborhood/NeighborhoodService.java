package com.example.project.global.neighborhood;

import java.util.List;
import java.util.stream.Collectors;

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
}