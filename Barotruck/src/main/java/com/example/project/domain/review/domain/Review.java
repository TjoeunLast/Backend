package com.example.project.domain.review.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter @Setter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    // 나중에 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shipment_id") 추가 예정
    @Column(nullable = false)
    private Long shipmentId; 

    @Column(nullable = false)
    private Long writerId; // 작성자 ID

    @Column(nullable = false)
    private Long targetId; // 리뷰 대상자 ID

    @Column(nullable = false)
    private Integer rating; // 1~5점

    @Column(length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}