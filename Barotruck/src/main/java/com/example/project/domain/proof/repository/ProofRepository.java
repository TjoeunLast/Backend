package com.example.project.domain.proof.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.domain.proof.domain.Proof;

public interface ProofRepository extends JpaRepository<Proof, Long> {
    // 주문 번호로 증빙 데이터 조회
    
//	Optional<Proof> findByOrder_OrderId(Long orderId);
}
