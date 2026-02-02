package com.example.project.domain.review.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.review.domain.Review;
import com.example.project.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    // private final UserRepository userRepository; // 나중에 주석 해제

    @Transactional
    public void createReview(Review review) {
        if (reviewRepository.existsByShipmentIdAndWriterId(review.getShipmentId(), review.getWriterId())) {
            throw new IllegalStateException("이미 이 운송건에 대한 리뷰를 작성하셨습니다.");
        }
        reviewRepository.save(review);
        updateTargetRating(review.getTargetId()); // 평점 업데이트 호출
    }
    

    // [R] 내 리뷰 혹은 대상자 리뷰 조회
    @Transactional(readOnly = true)
    public List<Review> getReviewsByTarget(Long targetId) {
        return reviewRepository.findByTargetId(targetId);
    }

    // [U] 리뷰 수정 (별점과 내용 변경 가능)
    @Transactional
    public void updateReview(Long reviewId, Integer newRating, String newContent) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
        
        review.setRating(newRating);
        review.setContent(newContent);
        // 저장 후 평점 다시 계산
        updateTargetRating(review.getTargetId());
        log.info("리뷰 수정 완료: ID {}", reviewId);
    }

    // [D] 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 존재하지 않습니다."));
        
        Long targetId = review.getTargetId();
        reviewRepository.delete(review);
        updateTargetRating(targetId); // 삭제 후 평점 재계산
        log.info("리뷰 삭제 완료: ID {}", reviewId);
    }

    // [기타] 평균 평점 업데이트 로직 (공통화)
    private void updateTargetRating(Long targetId) {
        /*
        Double avgRating = reviewRepository.getAverageRatingByTargetId(targetId);
        // UserRepository를 통해 유저의 ratingAvg 필드 업데이트 로직 수행
        log.info("사용자 ID {}의 평점 재계산 완료", targetId);
        */
    }
}
