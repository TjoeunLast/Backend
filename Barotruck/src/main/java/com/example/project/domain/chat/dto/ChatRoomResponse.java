package com.example.project.domain.chat.dto;

import java.time.LocalDateTime;

import com.example.project.domain.chat.domain.ChatMessage;
import com.example.project.domain.chat.domain.ChatRoom;
import com.example.project.domain.chat.domain.ChatRoomMember;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomResponse {
	private Long roomId;
    private String roomName;
    private ChatRoomType type;
    private String lastMessage;     // 추가: 최근 메시지 내용
    private LocalDateTime lastMessageTime; // 추가: 최근 메시지 시간
    private int unreadCount;        // (선택사항) 읽지 않은 메시지 수
    
    // ChatRoomMember 엔티티에서 ChatRoom 정보를 추출하여 생성하도록 수정
    public static ChatRoomResponse from(com.example.project.domain.chat.domain.ChatRoomMember member) {
        ChatRoom room = member.getRoom();
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .type(room.getType())
                .build();
    }
    
 // ChatRoomMember와 ChatMessage 정보를 조합하여 생성
    public static ChatRoomResponse of(ChatRoomMember member, ChatMessage lastMsg) {
        ChatRoom room = member.getRoom();
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .type(room.getType())
                .lastMessage(lastMsg != null ? lastMsg.getContent() : "메시지가 없습니다.")
                .lastMessageTime(lastMsg != null ? lastMsg.getCreatedAt() : room.getCreatedAt())
                .build();
    }
}
