package com.example.project.domain.review.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.domain.review.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
	// 1. 처리 상태별 조회 (PENDING, PROCESSING, RESOLVED)
    List<Report> findByStatus(String status);

    // 2. 특정 신고자(유저)가 올린 모든 신고 조회
    List<Report> findByReporterId(Long reporterId);

    // 3. 특정 운송건(shipment)과 관련된 모든 신고 조회
    List<Report> findByShipmentId(Long shipmentId);
    
//    나중에 신고건수가 많이질때 사용 
//    @Query("SELECT r FROM Report r WHERE r.status = :status ORDER BY r.createdAt DESC")
//    List<Report> findReportsByStatusOrderByLatest(@Param("status") String status);

}