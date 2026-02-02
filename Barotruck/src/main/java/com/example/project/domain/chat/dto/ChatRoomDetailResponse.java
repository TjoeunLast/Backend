package com.example.project.domain.chat.dto;

import java.util.List;

import com.example.project.domain.chat.domain.ChatRoom;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomDetailResponse {
    private Long roomId;
    private String roomName;
    private List<ChatMessageResponse> chatHistory;

    public static ChatRoomDetailResponse of(ChatRoom room, List<ChatMessageResponse> history) {
        return ChatRoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .chatHistory(history)
                .build();
    }
}
