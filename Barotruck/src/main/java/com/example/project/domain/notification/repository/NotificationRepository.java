package com.example.project.domain.notification.repository;

import com.example.project.domain.notification.domain.Notification;
import com.example.project.member.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 1. 특정 사용자의 모든 알림을 최신순으로 가져옵니다.
    List<Notification> findAllByUserOrderByCreatedAtDesc(Users user);

    // 2. 아직 읽지 않은 알림의 개수를 파악할 때 사용합니다.
    long countByUserAndReadAtIsNull(Users user);
}