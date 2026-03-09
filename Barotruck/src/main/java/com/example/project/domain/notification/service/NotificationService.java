package com.example.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.notification.domain.Notification; // 우리 엔티티를 기본으로 사용
import com.example.project.domain.notification.dto.NotificationResponse;
import com.example.project.domain.notification.repository.NotificationRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
// Firebase Notification import는 삭제합니다. (전체 경로를 직접 쓸 것이기 때문)

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UsersRepository usersRepository;

    // 🚩 핵심 1: REQUIRES_NEW를 적용하여, 여기서 에러가 나도 오더 상태 업데이트(본체)는 롤백되지 않게 격리합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(Users user, String type, String title, String body, Long targetId) {
        // 1. DB에 알림 내역 기록
        Notification notification = Notification.builder()
                .user(user) // 알림을 받는 사람
                .type(type)	// 타입
                .title(title) // 제목
                .body(body) // 내용
                .targetType(type) //타입2
                .targetId(targetId) // orderId
                .build();
        notificationRepository.save(notification);

        // 2. FCM 푸시 발송
        String token = user.getFcmToken();
        
        // 🚩 핵심 2: null 체크뿐만 아니라, 빈 문자열("")인지도 반드시 체크해야 Firebase가 터지지 않습니다!
        if (token != null && !token.trim().isEmpty()) {
            com.google.firebase.messaging.Notification fcmPayload = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(fcmPayload)
                    .putData("type", type)
                    .putData("targetId", String.valueOf(targetId))
                    .build();

            try {
                FirebaseMessaging.getInstance().sendAsync(message);
            } catch (Exception e) {
                // 발송 실패 시 서버가 뻗지 않도록 로그만 남깁니다.
                System.err.println("FCM 푸시 발송 실패: " + e.getMessage());
            }
        }
    }
    
    public List<NotificationResponse> getNotifications(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        notification.updateReadAt(LocalDateTime.now()); // 엔티티에 메서드 추가 필요
    }
}