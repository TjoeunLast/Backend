package com.example.project.domain.settlement.repository;

import com.example.project.domain.settlement.domain.Settlement;
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

    // 3. 상태별 정산 목록 조회 (예: READY, COMPLETED)
    List<Settlement> findByStatus(String status);

    /**
     * [관리자용] 기간별 요약 통계 집계
     * 수익 계산 수식: (totalPrice * feeRate / 100)
     */
    @Query("SELECT SUM(s.totalPrice), " +
           "SUM(s.totalPrice * s.feeRate / 100), " +
           "SUM(s.levelDiscount + s.couponDiscount), " +
           "COUNT(s) " +
           "FROM Settlement s " +
           "WHERE s.feeDate BETWEEN :start AND :end")
    Object[] getSettlementSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * [관리자용] 지역별 매출 및 정산 건수 집계 (Order의 Snapshot 정보 활용)
     */
    @Query("SELECT o.snapshot.puProvince, SUM(s.totalPrice), COUNT(s) " +
           "FROM Settlement s JOIN s.order o " +
           "WHERE s.feeDate BETWEEN :start AND :end " +
           "GROUP BY o.snapshot.puProvince " +
           "ORDER BY SUM(s.totalPrice) DESC")
    List<Object[]> getSettlementStatsByRegion(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}