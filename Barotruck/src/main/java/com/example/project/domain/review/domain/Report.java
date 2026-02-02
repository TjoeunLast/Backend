package com.example.project.domain.review.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports")
@Getter @Setter
@NoArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private Long shipmentId;

    @Column(nullable = false)
    private Long reporterId; // 신고자 ID
    
    @Column(nullable = false)
    private Long targetId; // 신고 대상자 ID

    @Column(nullable = false)
    private String reportType; // ACCIDENT, NO_SHOW, RUDE, ETC

    @Column(length = 2000, nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PROCESSING, RESOLVED

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
