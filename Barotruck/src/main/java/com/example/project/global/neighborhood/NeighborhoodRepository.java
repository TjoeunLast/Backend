package com.example.project.global.neighborhood;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long> {
    
    // displayName에 검색어가 포함된 모든 지역 리스트 조회
    List<Neighborhood> findByDisplayNameContaining(String query);

    // city_name(서울특별시 등)과 display_name(강남구 등) 모두에서 검색하고 싶을 때
    List<Neighborhood> findByCityNameContainingOrDisplayNameContaining(String cityName, String displayName);

	Optional<Neighborhood> findByNeighborhoodId(Long neighborhoodId);

	Optional<Neighborhood> findFirstBy();

    Optional<Neighborhood> findByCityNameAndDisplayName(String cityName, String displayName);

}
