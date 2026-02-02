package com.example.project.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomRequest {
    private String roomName;
    private ChatRoomType type; // PERSONAL, GROUP_BUY, FAMILY
    private Long postId;       // 공구/나눔 시 게시글 ID (일반 채팅 시 null)
}
