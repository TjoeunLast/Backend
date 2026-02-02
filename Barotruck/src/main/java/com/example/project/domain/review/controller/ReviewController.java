package com.example.project.domain.review.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.review.domain.Review;
import com.example.project.domain.review.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 1. 리뷰 등록
    @PostMapping
    public ResponseEntity<Boolean> createReview(@RequestBody Review review) {
        reviewService.createReview(review);
        return ResponseEntity.ok(true);
    }

    // 2. 특정 대상(차주/화주)의 리뷰 목록 조회
    @GetMapping("/target/{targetId}")
    public ResponseEntity<List<Review>> getReviewsByTarget(@PathVariable("targetId") Long targetId) {
        return ResponseEntity.ok(reviewService.getReviewsByTarget(targetId));
    }

    // 3. 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<Boolean> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content
    ) {
        reviewService.updateReview(reviewId, rating, content);
        return ResponseEntity.ok(true);
    }

    // 4. 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Boolean> deleteReview(@PathVariable("reviewId") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(true);
    }
}
