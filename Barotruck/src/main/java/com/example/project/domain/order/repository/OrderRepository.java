package com.example.project.domain.order.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.project.domain.order.domain.FarePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.project.domain.order.domain.Order;
import com.example.project.member.domain.Users;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    // 화주: 자신이 올린 요청 목록 조회
    List<Order> findByUserOrderByCreatedAtDesc(Users user);

    // 차주: 현재 매칭 대기 중인(REQUESTED) 전체 오더 조회
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    
 // 특정 상태 리스트에 포함된 오더 조회 (예: 모든 취소 상태)
    List<Order> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    
    
 // 1. 상태별 오더 개수 (대시보드 현황판용)
    long countByStatus(String status);

    // 2. 특정 기간 내 생성된 오더 전체 조회 (최신순)
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    // 3. 강제 배차(AdminControl)가 발생한 오더만 조회
    @Query("SELECT o FROM Order o JOIN FETCH o.adminControl WHERE o.adminControl IS NOT NULL")
    List<Order> findByForcedAllocatedOrders();

    // 4. 취소된 오더와 취소 정보 한 번에 조회 (Fetch Join으로 성능 최적화)
    @Query("SELECT o FROM Order o JOIN FETCH o.cancellationInfo WHERE o.status LIKE 'CANCELLED%'")
    List<Order> findAllWithCancellationDetails();

    // 5. 장기 미배차 오더 조회 (예: 생성된 지 1시간이 지났는데 아직 REQUESTED 상태인 오더)
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);
    
    // 6. 특정 관리자가 처리한 제어 내역 조회
    @Query("SELECT o FROM Order o JOIN o.adminControl ac WHERE ac.paidAdmin = :adminEmail")
    List<Order> findByAdminEmail(@Param("adminEmail") String adminEmail);
    
    /**
     * 출발지-도착지 쌍별 주문 건수 집계
     * OrderSnapshot(s) 내부의 puProvince와 doProvince를 사용합니다.
     */
    @Query("SELECT s.puProvince, s.doProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "GROUP BY s.puProvince, s.doProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersByRoute();
    
    
 // 1. 지역별 출발(Pickup) 건수 집계
    @Query("SELECT s.puProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "GROUP BY s.puProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersByPickupProvince();

    // 2. 지역별 도착(Drop-off) 건수 집계
    @Query("SELECT s.doProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "GROUP BY s.doProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersByDropoffProvince();
    
 // 1. 특정 기간 내 지역별 출발(Pickup) 건수 집계
    @Query("SELECT s.puProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY s.puProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countPickupByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. 특정 기간 내 지역별 도착(Drop-off) 건수 집계
    @Query("SELECT s.doProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY s.doProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countDropoffByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


 // 1. 노선별(출발-도착 쌍) 건수 집계
    @Query("SELECT s.puProvince, s.doProvince, COUNT(o) " +
           "FROM Order o JOIN o.snapshot s " +
           "WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY s.puProvince, s.doProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countRouteStatsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. 지역별 출발(Pickup) 건수 및 매출 합계
    @Query("SELECT s.puProvince, COUNT(o), SUM(s.basePrice + s.laborFee + s.packagingPrice + s.insuranceFee) " +
           "FROM Order o JOIN o.snapshot s " +
           "WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY s.puProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countPickupStatsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. 지역별 도착(Drop-off) 건수 및 매출 합계
    @Query("SELECT s.doProvince, COUNT(o), SUM(s.basePrice + s.laborFee + s.packagingPrice + s.insuranceFee) " +
           "FROM Order o JOIN o.snapshot s " +
           "WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY s.doProvince " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> countDropoffStatsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

 // OrderRepository.java
    @Query("SELECT o FROM Order o JOIN o.snapshot s " + 
           "WHERE o.status = 'REQUESTED' " +
           "AND (s.reqCarType = :carType OR s.reqCarType IS NULL) " +
           "AND s.tonnage <= :driverTonnage " + 
           "ORDER BY o.createdAt DESC")
    List<Order> findCustomOrders(
        @Param("carType") String carType, 
        @Param("driverTonnage") BigDecimal driverTonnage
    );
    
 // status가 String인 경우에도 In 키워드로 목록 조회가 가능합니다. 배차 현황중인 오더목록 볼수있는 (차주입장에서)
    List<Order> findByDriverNoAndStatusIn(Long driverNo, List<String> statuses); 
 
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    
    
}