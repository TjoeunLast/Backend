package com.example.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
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
    
    @Transactional
    public void sendNotification(Users user, String type, String title, String body, Long targetId) {
        // 1. DB에 알림 내역 기록 (우리 프로젝트의 Notification 엔티티 사용)
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .targetType(type)
                .targetId(targetId)
                .build();
        notificationRepository.save(notification);

     // 2. FCM 푸시 발송
        if (user.getFcmToken() != null) {
            // 여기에서 Firebase의 Notification을 전체 경로로 선언합니다.
            com.google.firebase.messaging.Notification fcmPayload = 
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(fcmPayload) // 생성한 페이로드를 넣습니다.
                    .putData("type", type) 
                    .putData("targetId", String.valueOf(targetId))
                    .build();

            try {
                FirebaseMessaging.getInstance().sendAsync(message); // 비동기 발송 추천
            } catch (Exception e) {
                e.printStackTrace();
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