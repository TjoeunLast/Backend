package com.example.project.domain.order.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.example.project.domain.order.domain.embedded.DriverTimeline;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.domain.Review;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.global.neighborhood.Neighborhood; // 지역코드 엔티티
import com.example.project.member.domain.Users; // 사용자 엔티티

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

    private String startPlace; // 판교 테크노벨리 제1공장	기사님이 현장에서 위치를 쉽게 식별하기 위한 명칭 (건물명 등)
    
    @Column(name = "START_TYPE")
    private String startType; // 당상, 익상, 야간상차

    @Column(name = "START_SCHEDULE", length = 20)
    private String startSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "START_NBH_ID")
    private Neighborhood startNeighborhood; // 상차지 지역코드 (FK)

    // 3. 지역 정보
    @Column(name = "PU_PROVINCE")
    private String puProvince; // 출발 주

    @Column(name = "DO_PROVINCE")
    private String doProvince; // 도착 주

    // 하차지 정보
    @Column(name = "END_ADDR", length = 500)
    private String endAddr;

    private String endPlace;
    
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
    
 // 2. 추가 비용 정보
    @Column(name = "PACKAGING_PRICE")
    private Long packagingPrice; // 포장비용

    @Column(name = "INSURANCE_FEE")
    private Long insuranceFee; // 보험료 (Field5)
    
    // 결제 관련 
    @Column(name = "PAY_METHOD", length = 20)
    private String payMethod; // 카드, 인수증 등

    // 상태 및 시간
    @Column(name = "STATUS", length = 30)
    private String status = "REQUESTED"; // 기본값 설정 ACCEPTED, LOADING(상차지), IN_TRANSIT(이동중), UNLOADING(하차지), COMPLETED

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    // 1. 거리 및 소요 시간 (물리적 지표)
    @Column(name = "DISTANCE")
    private Long distance; // 거리 (Field6)

    @Column(name = "DURATION")
    private Long duration; // 소요시간 (Field7)

    // 4. 시스템 공통
    @Column(name = "UPDATED")
    private LocalDateTime updated; // 업데이트 일시 (Field8)

    
    // Order.java 내부 추가
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Report> reports = new ArrayList<>();

 // [변경] 관리자 제어 (별도 테이블)
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AdminControl adminControl;

    // [변경] 취소 정보 (별도 테이블)
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CancellationInfo cancellationInfo;
    
 // [변경] 결제/정산 정보 (Settlement로 이동)
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Settlement settlement;
    
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
   
    // 편의 메서드 수정
    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
        if (settlement.getOrder() != this) settlement.setOrder(this);
    }

    public void setAdminControl(AdminControl adminControl) {
        this.adminControl = adminControl;
        if (adminControl.getOrder() != this) adminControl.setOrder(this);
    }

    public void setCancellationInfo(CancellationInfo cancellationInfo) {
        this.cancellationInfo = cancellationInfo;
        if (cancellationInfo.getOrder() != this) cancellationInfo.setOrder(this);
    }
    
 // Order.java 내부에 추가
    public void assignDriver(Long driverNo, String status) {
        this.driverNo = driverNo;
        this.status = status;
        this.updated = LocalDateTime.now();
    }

    public void cancelOrder(String status) {
        this.status = status;
        this.updated = LocalDateTime.now();
    }

    // 2. 기사 타임라인 묶음
    @Embedded
    private DriverTimeline driverTimeline;


    // Order.java 내부 추가
    public static Order createOrder(Users user, OrderRequest request) {

        return Order.builder()
                .user(user)
                .startAddr(request.getStartAddr())
                .startPlace(request.getStartPlace())
                .startType(request.getStartType())
                .startSchedule(request.getStartSchedule())
                .puProvince(request.getPuProvince())
                .endAddr(request.getEndAddr())
                .endPlace(request.getEndPlace())
                .endType(request.getEndType())
                .endSchedule(request.getEndSchedule())
                .doProvince(request.getDoProvince())
                .cargoContent(request.getCargoContent())
                .loadMethod(request.getLoadMethod())
                .workType(request.getWorkType())
                .tonnage(request.getTonnage())
                .reqCarType(request.getReqCarType())
                .reqTonnage(request.getReqTonnage())
                .driveMode(request.getDriveMode())
                .loadWeight(request.getLoadWeight())
                .basePrice(request.getBasePrice())
                .laborFee(request.getLaborFee())
                .packagingPrice(request.getPackagingPrice())
                .insuranceFee(request.getInsuranceFee())
                .payMethod(request.getPayMethod())
                .distance(request.getDistance())
                .duration(request.getDuration())
                .status("REQUESTED")
                .build();
    }
}