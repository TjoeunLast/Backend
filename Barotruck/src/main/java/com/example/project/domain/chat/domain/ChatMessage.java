package com.example.project.domain.chat.domain;

import com.example.project.global.controller.BaseTimeEntity;
import com.example.project.member.domain.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity { // BaseTimeEntity 상속으로 생성시간(sent_at) 확보

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private Users sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false, length = 20)
    private String type = "TEXT"; // TEXT, IMAGE, SYSTEM 등

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Builder
    public ChatMessage(Users sender, ChatRoom room, String type, String content) {
        this.sender = sender;
        this.room = room;
        this.type = type != null ? type : "TEXT";
        this.content = content;
    }
}