package com.example.project.domain.chat.domain;

import java.time.LocalDateTime;

import com.example.project.domain.chat.dto.MemberRole;
import com.example.project.member.domain.Users;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // 01-14 buyer 이렇게 넣으면 2명 제한에다가 나중에 확장시킬때 문제가 발생할거같아서 role 로 변경
    @Enumerated(EnumType.STRING)
    private MemberRole role; // OWNER(나눔이), PARTICIPANT(참여객)

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt; // 안 읽은 메시지 계산용

    @Builder
    public ChatRoomMember(Users user, ChatRoom room, MemberRole role) {
        this.user = user;
        this.room = room;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }
    
    // 읽음 시간 업데이트 로직
    public void updateLastRead() {
        this.lastReadAt = LocalDateTime.now();
    }
}