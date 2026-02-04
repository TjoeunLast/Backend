package com.example.project.global.neighborhood;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/neighborhoods")
@RequiredArgsConstructor
public class NeighborhoodController {

    private final NeighborhoodService neighborhoodService;

    /**
     * 키워드로 동네 검색 API
     * GET /api/neighborhoods/search?query=강남
     */
    @GetMapping("/search")
    public ResponseEntity<List<NeighborhoodResponse>> search(@RequestParam("query") String query) {
        if (query == null || query.trim().length() < 2) {
            // 최소 2글자 이상 입력하도록 제한 (선택 사항)
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(neighborhoodService.searchNeighborhoods(query));
    }
}