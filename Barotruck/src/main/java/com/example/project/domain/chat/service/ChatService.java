package com.example.project.domain.chat.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.chat.domain.ChatMessage;
import com.example.project.domain.chat.domain.ChatRoom;
import com.example.project.domain.chat.domain.ChatRoomMember;
import com.example.project.domain.chat.dto.ChatErrorCode;
import com.example.project.domain.chat.dto.ChatHistoryResponse;
import com.example.project.domain.chat.dto.ChatMessageRequest;
import com.example.project.domain.chat.dto.ChatMessageResponse;
import com.example.project.domain.chat.dto.ChatRoomDetailResponse;
import com.example.project.domain.chat.dto.ChatRoomRequest;
import com.example.project.domain.chat.dto.ChatRoomResponse;
import com.example.project.domain.chat.dto.ChatRoomType;
import com.example.project.domain.chat.dto.MemberRole;
import com.example.project.domain.chat.repository.ChatMessageRepository;
import com.example.project.domain.chat.repository.ChatRoomMemberRepository;
import com.example.project.domain.chat.repository.ChatRoomRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UsersRepository usersRepository;

    

    // 내 채팅방 목록 조회
//    public List<ChatRoomResponse> getMyRooms(Long userId) {
//        return chatRoomMemberRepository.findAllByUserUserId(userId).stream()
//                .map(ChatRoomResponse::from) // ChatRoomResponse.from(ChatRoomMember) 호출
//                .collect(Collectors.toList());
//    }

    public List<ChatRoomResponse> getMyRooms(Long userId) {
        return chatRoomMemberRepository.findAllByUserUserId(userId).stream()
                .map(member -> {
                    // 1. 해당 방의 최신 메시지 조회
                    ChatMessage lastMessage = chatMessageRepository
                        .findFirstByRoomRoomIdOrderByCreatedAtDesc(member.getRoom().getRoomId())
                        .orElse(null);

                    // 2. static 메서드를 통해 DTO 변환
                    return ChatRoomResponse.of(member, lastMessage);
                })
                // 3. 최신 메시지 시간 순으로 정렬 (최신순)
                .sorted(Comparator.comparing(ChatRoomResponse::getLastMessageTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
    
    
    // 방 상세 조회 및 읽음 처리
    @Transactional
    public ChatRoomDetailResponse getRoomDetail(Long roomId, Long userId) {
        // 1. 방 존재 여부 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(ChatErrorCode.ROOM_NOT_FOUND.getMessage()));
        
        // 2. 참여 권한 확인 (가장 중요!)
        ChatRoomMember member = chatRoomMemberRepository.findByRoomRoomIdAndUserUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException(ChatErrorCode.ACCESS_DENIED.getMessage()));

        // 3. 읽음 시간 갱신
        member.updateLastRead(); 

        List<ChatMessageResponse> history = chatMessageRepository.findAllByRoomRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());

        return ChatRoomDetailResponse.of(room, history);
    }

    // 메시지 저장
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId()).orElseThrow();
        Users sender = usersRepository.findByUserId(request.getSenderId()).orElseThrow();

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .build();
        chatMessageRepository.save(message);

        return ChatMessageResponse.fromEntity(message);
    }

    // 방 나가기
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        chatRoomMemberRepository.deleteByRoomRoomIdAndUserUserId(roomId, userId);
        
        // 만약 방에 남은 인원이 없다면 방 삭제 로직 추가 가능
        if (!chatRoomMemberRepository.existsByRoomRoomId(roomId)) {
            chatRoomRepository.deleteById(roomId);
        }
    }
    
    /**
     * 채팅방 입장 및 대화 이력 조회
     */
    @Transactional // 읽음 시간 업데이트(Dirty Checking)를 위해 Transactional 필요
    public ChatHistoryResponse getChatHistory(Long roomId, Long userId, Pageable pageable) {
        
        // 1. 해당 방에 참여 중인 유저인지 권한 확인
        ChatRoomMember member = chatRoomMemberRepository.findByRoomRoomIdAndUserUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("해당 채팅방에 접근 권한이 없습니다."));

        // 2. 읽음 상태 업데이트 (마지막 읽은 시간을 현재 시간으로 갱신)
        // 이 처리를 통해 Flutter 앱 목록의 '안 읽은 메시지 수'가 0으로 초기화됩니다.
        member.updateLastRead();

        // 3. 메시지 이력을 최신순으로 페이징 조회 (Slice 사용)
        // repository에서 findAllByRoomRoomIdOrderByCreatedAtDesc 로 선언했어야 함
        Slice<ChatMessage> messageSlice = chatMessageRepository
                .findAllByRoomRoomIdOrderByCreatedAtDesc(roomId, pageable);

        // 4. DTO 변환 및 반환
        return ChatHistoryResponse.of(roomId, messageSlice);
    }
    
    
    
    /**
     * 특정 채팅방의 과거 메시지 전체 조회
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId) {
        // 1. DB에서 해당 방의 메시지 리스트 조회
        List<ChatMessage> messages = chatMessageRepository.findByRoomRoomIdOrderByCreatedAtAsc(roomId);

        // 2. Entity -> Response DTO 변환 후 반환
        return messages.stream()
                .map(ChatMessageResponse::fromEntity)
                .toList();
    }
    
    
//    @Transactional
//    public Long createGroupBuyChatRoom(Long userId, Long postId) {
//        Users creator = usersRepository.findById(userId).orElseThrow();
//        GroupBuyPost post = postRepository.findById(postId).orElseThrow();
//
//        ChatRoom chatRoom = ChatRoom.builder()
//                .type(ChatRoomType.GROUP_BUY)
//                .post(post)
//                .roomName("[공구] " + post.getTitle())
//                .build();
//        chatRoomRepository.save(chatRoom);
//
//        chatRoomMemberRepository.save(ChatRoomMember.builder()
//                .user(creator)
//                .room(chatRoom)
//                .role(MemberRole.OWNER)
//                .build());
//
//        return chatRoom.getRoomId();
//    }
}