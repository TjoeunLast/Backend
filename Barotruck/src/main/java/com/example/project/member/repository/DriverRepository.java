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
    
 // 이 메서드를 추가해야 OrderService에서 사용할 수 있습니다.
    // 엔티티 내의 연관관계 필드명(user) + 해당 엔티티의 ID 필드명(userId) 조합입니다.
    Optional<Driver> findByUser_UserId(Long userId);
}