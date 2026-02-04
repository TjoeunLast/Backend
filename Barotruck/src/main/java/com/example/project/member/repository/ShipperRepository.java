package com.example.project.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.Shipper;
import com.example.project.member.domain.Users;

@Repository
public interface ShipperRepository extends JpaRepository<Shipper, Long> {
    // 특정 유저 엔티티로 화주 프로필 찾기
    Optional<Shipper> findByUser(Users user);
    
    // 사업자 등록 번호로 중복 체크 시 사용
    boolean existsByBizRegNum(String bizRegNum);
}
