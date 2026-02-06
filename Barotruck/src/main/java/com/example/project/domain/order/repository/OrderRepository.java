package com.example.project.domain.order.repository;

import com.example.project.domain.order.domain.Order;
import com.example.project.member.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 화주: 자신이 올린 요청 목록 조회
    List<Order> findByUserOrderByCreatedAtDesc(Users user);

    // 차주: 현재 매칭 대기 중인(REQUESTED) 전체 오더 조회
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    
 // 특정 상태 리스트에 포함된 오더 조회 (예: 모든 취소 상태)
    List<Order> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}