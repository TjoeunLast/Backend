package com.example.project.domain.notification.dto;

import com.example.project.domain.notification.domain.Notification;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String type;
    private String title;
    private String body;
    private Long targetId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .targetId(n.getTargetId())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
