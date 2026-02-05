package com.example.project.domain.review.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewRequestDto {
    private Long orderId;   // 어느 오더에 대한 리뷰인지
    private Integer rating; // 1~5점
    private String content; // 리뷰 내용
}