package com.example.project.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private String type; // TEXT, IMAGE, SYSTEM
    private LocalDateTime createdAt;

    public static ChatMessageResponse fromEntity(com.example.project.domain.chat.domain.ChatMessage entity) {
        return ChatMessageResponse.builder()
                .messageId(entity.getMessageId())
                .senderId(entity.getSender().getUserId())
                .senderNickname(entity.getSender().getNickname())
                .content(entity.getContent())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
