package com.example.project.domain.review.domain;

import java.time.LocalDateTime;

import com.example.project.domain.order.domain.Order;
import com.example.project.member.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

 // 신고와 관련된 오더 (shipmentId 대신 orderId 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 신고자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Users reporter;
    
    // 신고 대상자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Users target;

    @Column(nullable = false)
    private String reportType; // ACCIDENT, NO_SHOW, RUDE, ETC

    @Column(length = 2000, nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PROCESSING, RESOLVED

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
