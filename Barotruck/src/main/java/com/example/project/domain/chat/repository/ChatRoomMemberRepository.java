package com.example.project.domain.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.project.domain.chat.domain.ChatRoomMember;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    /**
     * 1. 유저 ID로 참여 중인 방 목록 조회
     * ChatRoomMember 내의 User 엔티티 안의 userId를 참조해야 함
     */
    List<ChatRoomMember> findAllByUserUserId(Long userId);

    /**
     * 2. 특정 방에 특정 유저가 있는지 확인 (권한 체크용)
     */
    Optional<ChatRoomMember> findByRoomRoomIdAndUserUserId(Long roomId, Long userId);

    /**
     * 3. 특정 방에 유저가 참여 중인지 여부 확인
     */
    boolean existsByRoomRoomIdAndUserUserId(Long roomId, Long userId);

    /**
     * 4. 방에 남은 인원이 있는지 확인 (방 삭제 로직용)
     */
    boolean existsByRoomRoomId(Long roomId);

    /**
     * 5. 방 나가기 처리 (삭제)
     */
    void deleteByRoomRoomIdAndUserUserId(Long roomId, Long userId);
    
    /**
     * 6. 안 읽은 메시지 개수 계산
     * @Param 어노테이션을 명시하는 것이 안전합니다.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room.roomId = :roomId AND m.createdAt > :lastReadAt")
    Long countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadAt") LocalDateTime lastReadAt);

 // 1대1 개인 채팅방이 이미 존재하는지 찾는 쿼리
    @Query("SELECT m1.room.roomId FROM ChatRoomMember m1 " +
           "JOIN ChatRoomMember m2 ON m1.room = m2.room " +
           "WHERE m1.user.userId = :userId1 " +
           "AND m2.user.userId = :userId2 " +
           "AND m1.room.type = 'PERSONAL'")
    Optional<Long> findPersonalRoomBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
	
	
    // 기존에 에러를 유발하던 deleteByRoomIdAndUserId 등 잘못된 메서드는 삭제했습니다.
}