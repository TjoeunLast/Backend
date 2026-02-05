package com.example.project.domain.review.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.review.domain.Review;
import com.example.project.domain.review.dto.ReviewRequestDto;
import com.example.project.domain.review.dto.ReviewResponseDto;
import com.example.project.domain.review.repository.ReviewRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
     private final UsersRepository userRepository; // 나중에 주석 해제
     private final OrderRepository orderRepository; // 추가 필요
     
     
     @Transactional
     public void createReview(ReviewRequestDto dto, Users currentUser) {
         Order order = orderRepository.findById(dto.getOrderId())
                 .orElseThrow(() -> new IllegalArgumentException("해당 오더가 없습니다."));

         // 공통 함수로 상대방(Target) 특정
         Long targetId = order.getOpponentId(currentUser.getUserId());
         Users target = userRepository.findById(targetId)
                 .orElseThrow(() -> new IllegalArgumentException("리뷰 대상자를 찾을 수 없습니다."));

         // 중복 작성 방지
         if (reviewRepository.existsByOrder_OrderIdAndWriter_UserId(order.getOrderId(), currentUser.getUserId())) {
             throw new IllegalStateException("이미 이 운송건에 대한 리뷰를 작성하셨습니다.");
         }

         Review review = Review.builder()
                 .order(order)
                 .writer(currentUser)
                 .target(target)
                 .rating(dto.getRating())
                 .content(dto.getContent())
                 .createdAt(LocalDateTime.now())
                 .build();

         reviewRepository.save(review);
         updateTargetRating(targetId);
     }

    // [U] 리뷰 수정 (별점과 내용 변경 가능)
    @Transactional
    public void updateReview(Long reviewId, Integer newRating, String newContent) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
        
        review.setRating(newRating);
        review.setContent(newContent);
        
        updateTargetRating(review.getTarget().getUserId());
    }
    // 사용자의 리뷰 수정 로직
    @Transactional
    public void updateReview(Long reviewId, Integer newRating, String newContent, Users currentUser) {
    	Review review = reviewRepository.findById(reviewId)
    			.orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
    	
    	// 본인 확인: 리뷰 작성자의 ID와 현재 로그인한 유저의 ID 비교
    	if (!review.getWriter().getUserId().equals(currentUser.getUserId())) {
    		throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
    	}
    	
    	review.setRating(newRating);
    	review.setContent(newContent);
    	
    	// 대상자의 평균 평점 재계산
    	updateTargetRating(review.getTarget().getUserId());
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByTarget(Long targetId) {
    	return reviewRepository.findByTarget_UserId(targetId).stream()
    			.map(ReviewResponseDto::new) // 엔티티 -> DTO 변환
    			.collect(Collectors.toList());
    }

    // [D] 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
        
        Long targetId = review.getTarget().getUserId();
        reviewRepository.delete(review);
        updateTargetRating(targetId);
    }
    // 사용자의 리뷰 삭제 로직
    @Transactional
    public void deleteReview(Long reviewId, Users currentUser) {
    	Review review = reviewRepository.findById(reviewId)
    			.orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
    	
    	// 본인 확인
    	if (!review.getWriter().getUserId().equals(currentUser.getUserId())) {
    		throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
    	}
    	
    	Long targetId = review.getTarget().getUserId();
    	reviewRepository.delete(review);
    	
    	// 삭제 후 평균 평점 업데이트
    	updateTargetRating(targetId);
    }

    // [기타] 평균 평점 업데이트 로직 (공통화)
    private void updateTargetRating(Long targetId) {
        Double avgRating = reviewRepository.getAverageRatingByTargetId(targetId);
        if (avgRating != null) {
            userRepository.findById(targetId).ifPresent(user -> {
                user.setRatingAvg(Math.round(avgRating)); // Users 엔티티의 필드 타입에 맞춰 변환
            });
        }
    }
    

}
