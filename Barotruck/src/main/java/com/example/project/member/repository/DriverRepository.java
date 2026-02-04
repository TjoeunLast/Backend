package com.example.project.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    // 특정 유저 엔티티로 차주 프로필 찾기
    Optional<Driver> findByUser(Users user);
    
    // 차량 번호로 중복 체크 시 사용
    boolean existsByCarNum(String carNum);
}