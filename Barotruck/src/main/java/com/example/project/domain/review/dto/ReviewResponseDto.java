package com.example.project.domain.review.dto;

import com.example.project.domain.review.domain.Review;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ReviewResponseDto {
    private Long reviewId;
    private String writerNickname; // 작성자 이름 (Users 엔티티 활용)
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

    public ReviewResponseDto(Review review) {
        this.reviewId = review.getReviewId();
        this.writerNickname = review.getWriter().getNickname();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.createdAt = review.getCreatedAt();
    }
}