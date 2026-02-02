package com.example.project.domain.chat.domain;

import com.example.project.domain.chat.dto.ChatRoomType;
import com.example.project.global.controller.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder 사용을 위해 추가
@Builder // 클래스 레벨로 이동
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

//    // 01-14 post 기존에는 null 허용 안했던데 그러면 문제가 공구만 이 채팅방을 사용할 수있다는건데 
//    // 기존 설계대로랑 어긋나는 측면에 있어서 널허용을 바꿈
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "post_id", nullable = true) // null이면 일반 채팅, 존재하면 공구/나눔 채팅
//    private GroupBuyPost post;
    
    // 01-14 공구,개인, 기타 등등 목적에 맞게 사용되도록 타입 이넘타입 추가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default  // 빌더 패턴 사용 시 초기값을 유지하도록 설정
    private ChatRoomType type = ChatRoomType.PERSONAL;
    
    @Column(length = 100)
    private String roomName; // 채팅방 이름 (가족 채팅방 등에서 활용)

    @Builder
    public ChatRoom(ChatRoomType type, String roomName) {
        this.type = type;
        this.roomName = roomName;
    }

}