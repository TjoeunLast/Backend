package com.example.project.global.fcm;


import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FCMService {

    /**
     * 특정 기기(Token)로 알림 전송
     */
    public void sendNotification(String token, String title, String body) {
        if (token == null || token.isEmpty()) {
            log.warn("FCM Token is empty. Skipping notification.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Sent message: " + response);
        } catch (Exception e) {
            log.error("FCM Send Error: ", e);
        }
    }
}