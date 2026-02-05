package com.example.project.domain.review.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.domain.review.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 내가 쓴 리뷰 목록
    List<Review> findByWriter_UserId(Long userId);

    // 특정 사용자에게 달린 리뷰 목록
    List<Review> findByTarget_UserId(Long userId);

    // 특정 오더에 대해 이미 리뷰를 썼는지 확인
    boolean existsByOrder_OrderIdAndWriter_UserId(Long orderId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.target.userId = :targetId")
    Double getAverageRatingByTargetId(@Param("targetId") Long targetId);
    
}
