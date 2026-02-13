package com.example.project.domain.order.domain;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.example.project.domain.order.domain.embedded.DriverTimeline;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.review.domain.Report;
import com.example.project.domain.review.domain.Review;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.member.domain.Users; // 사용자 엔티티

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

 // 1. 배차를 희망하는 기사들의 ID 목록 (신청자 명단)
    @ElementCollection
    @CollectionTable(
        name = "ORDER_DRIVER_LIST", // 별도의 보조 테이블 생성
        joinColumns = @JoinColumn(name = "ORDER_ID")
    )
    @Column(name = "DRIVER_ID")
    @Builder.Default
    private List<Long> driverList = new ArrayList<>();
    
    
    // 화주 (userId - FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private Users user;

 // 본문 정보 (한 번 정해지면 거의 안 바뀜)
    @Embedded
    private OrderSnapshot snapshot;
    // 1. 거리 및 소요 시간 (물리적 지표)
    @Column(name = "DISTANCE")
    private Long distance; // 거리 (Field6)
    
    @Column(name = "DURATION")
    private Long duration; // 소요시간 (Field7)

    // 상태 및 시간
    @Column(name = "STATUS", length = 30)
    private String status = "REQUESTED"; // 기본값 설정 ACCEPTED, LOADING(상차지), IN_TRANSIT(이동중), UNLOADING(하차지), COMPLETED

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
    
    
 // Order.java 내부에 추가
    public void changeStatus(String newStatus) {
        validateStatusTransition(newStatus); // 필요 시 상태 전환 규칙 검증 로직 추가
        this.status = newStatus;
        this.updated = LocalDateTime.now(); // 상태 변경 시 시간 갱신
    }

    /**
     * 상태 변경 시 비즈니스 규칙 검증 (선택 사항)
     */
    private void validateStatusTransition(String nextStatus) {
        // 예: COMPLETED 상태에서는 더 이상 변경 불가 등
        if ("COMPLETED".equals(this.status)) {
            throw new IllegalStateException("이미 완료된 주문은 상태를 변경할 수 없습니다.");
        }
        
        // 이외에 ACCEPTED -> LOADING -> IN_TRANSIT 등의 순서 검증 로직을 넣을 수 있습니다.
    }
    
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

 // Order.java 내부
    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt; // 이 필드가 반드시 Order 엔티티 직속으로 있어야 합니다.

    
 // 응답용 내부 DTO
    @Getter @AllArgsConstructor
    public static class RouteStatisticsResponse {
        private String startProvince;
        private String endProvince;
        private long orderCount;
    }
    
    @Getter @AllArgsConstructor
    public static class ProvinceStatResponse {
        private String province;
        private long count;
    }
 // --- 응답 DTO ---
    @Getter @AllArgsConstructor
    public static class RouteStatResponse {
        private String startProvince;
        private String endProvince;
        private long count;
    }

    @Getter @AllArgsConstructor
    public static class ProvinceAnalysisResponse {
        private String province;
        private long orderCount;
        private long totalSales; // 해당 지역의 총 매출액
    }
    
    /**
     * 기사가 배차 신청을 했을 때 목록에 추가
     */
    public void addToDriverList(Long driverId) {
        if (!this.driverList.contains(driverId)) {
            this.driverList.add(driverId);
        }
    }

    /**
     * 목록에 있는 기사 중 한 명을 최종 배정
     */
    public void confirmDriver(Long selectedDriverId) {
        if (this.driverList.contains(selectedDriverId)) {
            this.driverNo = selectedDriverId;
            this.changeStatus("ACCEPTED"); // 상태를 배차 완료로 변경
        } else {
            throw new IllegalArgumentException("신청자 명단에 없는 기사입니다.");
        }
    }
    
    
    public static Order createOrder(Users user, OrderRequest request) {
        // 1. 변하지 않는 상세 정보를 한데 묶음
        OrderSnapshot snapshot = OrderSnapshot.builder()
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
                .instant(request.isInstant())
                .build();

        // 2. 최종 Order 객체 생성
        return Order.builder()
                .user(user)
                .snapshot(snapshot) // 묶은 덩어리를 한 번에 주입
                .distance(request.getDistance())
                .duration(request.getDuration())
                .status("REQUESTED")
                .build();
    }
}