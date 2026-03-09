package com.example.project.domain.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.review.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 1. 처리 상태별 조회 (PENDING, PROCESSING, RESOLVED)
    List<Report> findByStatus(String status);

    // 2. 특정 신고자(유저)가 올린 모든 신고 조회
    List<Report> findByReporter_UserId(Long userId);

    // 특정 오더와 관련된 신고 조회 (order 필드의 orderId 접근)
    List<Report> findByOrder_OrderId(Long orderId);

    // 전체 신고 목록 최신순 조회
    List<Report> findAllByOrderByCreatedAtDesc();

    // 내가 신고한 목록 최신순 조회
    List<Report> findByReporter_UserIdOrderByCreatedAtDesc(Long userId);

	List<Report> findByType(String type);
}