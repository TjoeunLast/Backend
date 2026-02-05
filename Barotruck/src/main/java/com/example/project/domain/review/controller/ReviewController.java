package com.example.project.domain.review.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.review.dto.ReviewRequestDto;
import com.example.project.domain.review.dto.ReviewResponseDto;
import com.example.project.domain.review.service.ReviewService;
import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

 // 1. 리뷰 등록 (DTO 사용)
    @PostMapping
    public ResponseEntity<Boolean> createReview(
            @RequestBody ReviewRequestDto dto,
            @AuthenticationPrincipal Users currentUser // 보안: 세션에서 유저 정보 직접 획득
    ) {
        reviewService.createReview(dto, currentUser);
        return ResponseEntity.ok(true);
    }

    // 2. 특정 대상의 리뷰 목록 조회 (ResponseDto 반환)
    @GetMapping("/target/{targetId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByTarget(
    		@PathVariable("targetId") Long targetId) {
        return ResponseEntity.ok(reviewService.getReviewsByTarget(targetId));
    }

    // 3. 관리자 리뷰 수정
    @PutMapping("/admin/{reviewId}")
    public ResponseEntity<Boolean> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody ReviewRequestDto dto, // 수정 시에도 DTO나 별도 필드 사용 가능
            @AuthenticationPrincipal Users currentUser // 보안: 세션에서 유저 정보 직접 획득
    ) {
    	if(currentUser.getRole() != Role.ADMIN) {
    	    return ResponseEntity.ok(false);
    	}
    	reviewService.updateReview(reviewId, dto.getRating(), dto.getContent());
        return ResponseEntity.ok(true);
    }

    // 4. 관리자 리뷰 삭제
    @DeleteMapping("/admin/{reviewId}")
    public ResponseEntity<Boolean> deleteReview(
    		@PathVariable("reviewId") Long reviewId,
            @AuthenticationPrincipal Users currentUser // 보안: 세션에서 유저 정보 직접 획득
    		) {
    	if(currentUser.getRole() != Role.ADMIN) {
    	    return ResponseEntity.ok(false);
    	}
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(true);
    }
    
 // 3-1. 사용자가 직접 본인의 리뷰 수정
    @PutMapping("/my/{reviewId}")
    public ResponseEntity<Boolean> updateMyReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody ReviewRequestDto dto,
            @AuthenticationPrincipal Users currentUser
    ) {
    	reviewService.updateReview(reviewId, dto.getRating(), dto.getContent(), currentUser);
        return ResponseEntity.ok(true);
    
    }

    // 4-1. 사용자가 직접 본인의 리뷰 삭제
    @DeleteMapping("/my/{reviewId}")
    public ResponseEntity<Boolean> deleteMyReview(
            @PathVariable("reviewId") Long reviewId,
            @AuthenticationPrincipal Users currentUser
    ) {
        reviewService.deleteReview(reviewId, currentUser);
        return ResponseEntity.ok(true);
    }
}
