package com.example.project.domain.review.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.domain.review.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {


 // 1. 내가 쓴 리뷰 목록 조회
    List<Review> findByWriterId(Long writerId);

    // 2. 특정 사용자(차주/화주)에게 달린 리뷰 목록 조회
    List<Review> findByTargetId(Long targetId);

    // 3. 특정 운송건에 대한 리뷰가 이미 있는지 확인 (중복 등록 방지)
    boolean existsByShipmentIdAndWriterId(Long shipmentId, Long writerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.targetId = :targetId")
    Double getAverageRatingByTargetId(@Param("targetId") Long targetId);

}
