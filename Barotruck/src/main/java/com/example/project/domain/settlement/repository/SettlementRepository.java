package com.example.project.domain.settlement.repository;

import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 1. 특정 주문(Order)에 연결된 정산 정보 조회
    Optional<Settlement> findByOrder_OrderId(Long orderId);

    // 2. 특정 사용자의 정산 내역 조회 (최신순)
    List<Settlement> findByUser_UserIdOrderByFeeDateDesc(Long userId);

    @Query("SELECT s " +
           "FROM Settlement s JOIN s.order o " +
           "WHERE o.driverNo = :driverUserId " +
           "ORDER BY s.feeDate DESC")
    List<Settlement> findByDriverUserIdOrderByFeeDateDesc(@Param("driverUserId") Long driverUserId);

    // 3. 상태별 정산 목록 조회 (예: READY, COMPLETED)
    List<Settlement> findByStatus(SettlementStatus status);

    /**
     * [관리자용] 기간별 요약 통계 집계
     * 신규 건은 저장된 snapshot을 우선 사용하고, 구버전 데이터만 레거시 필드로 폴백한다.
     */
    @Query("SELECT SUM(COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0)), " +
           "SUM(COALESCE(s.platformNetRevenueSnapshot, s.platformGrossRevenueSnapshot, (s.totalPrice * s.feeRate / 100), 0)), " +
           "SUM(COALESCE(s.levelDiscount, 0) + COALESCE(s.couponDiscount, 0)), " +
           "COUNT(s) " +
           "FROM Settlement s " +
           "WHERE s.feeDate BETWEEN :start AND :end")
    Object[] getSettlementSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0)), 0), " +
           "COALESCE(SUM(CASE WHEN s.status <> :completedStatus THEN COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0) ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN s.status = :completedStatus THEN COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0) ELSE 0 END), 0), " +
           "COUNT(s), " +
           "COALESCE(SUM(CASE WHEN s.status <> :completedStatus THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN s.status = :completedStatus THEN 1 ELSE 0 END), 0) " +
           "FROM Settlement s")
    Object[] getSettlementStatusSummary(@Param("completedStatus") SettlementStatus completedStatus);

    /**
     * [관리자용] 지역별 매출 및 정산 건수 집계 (Order의 Snapshot 정보 활용)
     */
    @Query("SELECT o.snapshot.puProvince, SUM(COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0)), COUNT(s) " +
           "FROM Settlement s JOIN s.order o " +
           "WHERE s.feeDate BETWEEN :start AND :end " +
           "GROUP BY o.snapshot.puProvince " +
           "ORDER BY SUM(COALESCE(s.shipperChargeAmountSnapshot, s.totalPrice, 0)) DESC")
    List<Object[]> getSettlementStatsByRegion(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s " +
           "FROM Settlement s " +
           "WHERE s.order.orderId = :orderId")
    Optional<Settlement> findByOrderId(@Param("orderId") Long orderId);
}
