package com.example.project.domain.order.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.domain.Review;
import com.example.project.global.neighborhood.Neighborhood; // 지역코드 엔티티
import com.example.project.member.domain.Users; // 사용자 엔티티

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ORDERS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;

    // 차주 (DRIVER_NO)
    @Column(name = "DRIVER_NO", nullable=true)
    private Long driverNo;

    // 화주 (userId - FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private Users user;

    // 상차지 정보
    @Column(name = "START_ADDR", length = 500)
    private String startAddr;

    @Column(name = "START_TYPE")
    private String startType; // 당상, 익상, 야간상차

    @Column(name = "START_SCHEDULE", length = 20)
    private String startSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "START_NBH_ID")
    private Neighborhood startNeighborhood; // 상차지 지역코드 (FK)

    // 하차지 정보
    @Column(name = "END_ADDR", length = 500)
    private String endAddr;

    @Column(name = "END_TYPE")
    private String endType; // 당착, 내착

    @Column(name = "END_SCHEDULE", length = 20, nullable=true)
    private String endSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "END_NBH_ID")
    private Neighborhood endNeighborhood; // 하차지 지역코드 (FK)

    // 화물 및 작업 정보
    @Column(name = "CARGO_CONTENT", length = 500, nullable=true)
    private String cargoContent;

    @Column(name = "LOAD_METHOD", length = 50)
    private String loadMethod; // 독차, 혼적

    @Column(name = "WORK_TYPE", length = 50, nullable=true)
    private String workType; // 지게차 상/하차, 수작업

    @Column(name = "TONNAGE", precision = 4, scale = 1)
    private BigDecimal tonnage;

    @Column(name = "REQ_CAR_TYPE", length = 50, nullable=true)
    private String reqCarType; // 윙바디, 카고 등

    @Column(name = "REQ_TONNAGE", nullable=true)
    private String reqTonnage;

    @Column(name = "DRIVE_MODE", length = 20)
    private String driveMode; // 편도, 왕복

    @Column(name = "LOAD_WEIGHT")
    private Long loadWeight;

    // 요금 정보
    @Column(name = "BASE_PRICE")
    private Long basePrice;

    @Column(name = "LABOR_FEE", nullable=true)
    private Long laborFee;

    @Column(name = "PAY_METHOD", length = 20)
    private String payMethod; // 카드, 인수증 등

    @Column(name = "FEE_RATE", nullable=true)
    private Long feeRate;

    @Column(name = "TOTAL_PRICE")
    private Long totalPrice;

    // 상태 및 시간
    @Column(name = "STATUS", length = 30)
    private String status = "REQUESTED"; // 기본값 설정

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    
    
 // Order.java 내부 추가
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Report> reports = new ArrayList<>();

 // 리뷰 추가 편의 메서드
    public void addReview(Review review) {
        this.reviews.add(review);
        if (review.getOrder() != this) {
            review.setOrder(this);
        }
    }

    // 신고 추가 편의 메서드
    public void addReport(Report report) {
        this.reports.add(report);
        if (report.getOrder() != this) {
            report.setOrder(this);
        }
    }
    
    public Long getOpponentId(Long currentUserId) {
        if (this.user.getUserId().equals(currentUserId)) {
            // 내가 화주라면 상대방은 차주(driverNo)
            if (this.driverNo == null) {
                throw new IllegalStateException("아직 배차된 차주가 없습니다.");
            }
            return this.driverNo;
        } else if (this.driverNo != null && this.driverNo.equals(currentUserId)) {
            // 내가 차주라면 상대방은 화주(user.userId)
            return this.user.getUserId();
        } else {
            throw new IllegalStateException("해당 오더의 당사자가 아닙니다.");
        }
    }
    
    // 경유지는 별도 도메인으로 구현 예정이므로 여기서는 제외하거나 
    // 일대다(OneToMany) 관계로 추후 추가 가능합니다.
}