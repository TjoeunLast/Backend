package com.example.project.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.domain.chat.domain.ChatMessage;
import com.example.project.domain.chat.domain.ChatRoom;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 모든 메시지를 시간순으로 조회
    // 정렬 기준은 BaseTimeEntity의 생성 시간(createdAt)을 사용합니다.
    List<ChatMessage> findAllByRoomRoomIdOrderByCreatedAtAsc(Long roomId);
    Slice<ChatMessage> findAllByRoomRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
	
    List<ChatMessage> findByRoomRoomIdOrderByCreatedAtAsc(Long roomId);
    
    // 
    /**
     * 특정 채팅방(roomId)에서 가장 최근에 작성된 메시지 1개를 가져옵니다.
     * findFirstBy : 첫 번째 데이터만 가져옴
     * Room_RoomId : ChatMessage 엔티티 내부의 Room 객체 안에 있는 roomId 필드를 참조
     * OrderByCreatedAtDesc : 생성일자(createdAt) 기준 내림차순 정렬
     */
	Optional<ChatMessage> findFirstByRoomRoomIdOrderByCreatedAtDesc(Long roomId);
    
    // (선택) 최신 메시지 50개만 가져오기 (성능 최적화용)
    // List<ChatMessage> findTop50ByRoomRoomIdOrderByCreatedAtDesc(Long roomId);
}
