package com.example.project.global.neighborhood;


import java.util.List;

import com.example.project.global.neighborhood.dto.NeighborhoodRequest;
import com.example.project.global.neighborhood.dto.NeighborhoodResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    //
    @PostMapping("/findByAddress")
    public ResponseEntity<NeighborhoodResponse> resolve(
            @RequestBody NeighborhoodRequest request
    ) {

        return ResponseEntity.ok(
            neighborhoodService.resolveNeighborhood(
                request.getCityName(),
                request.getDisplayName()
            )
        );
    }
    @PostMapping("/findByFullAddress")
    public ResponseEntity<NeighborhoodResponse> resolveByFullAddress(
            @RequestBody NeighborhoodRequest request
    ) {

        return ResponseEntity.ok(
            neighborhoodService.resolveNeighborhood(
                request.getFullAddress()
            )
        );
    }

}