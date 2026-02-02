package com.example.project.domain.chat.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.chat.domain.ChatRoom;
import com.example.project.domain.chat.domain.ChatRoomMember;
import com.example.project.domain.chat.dto.ChatRoomType;
import com.example.project.domain.chat.dto.MemberRole;
import com.example.project.domain.chat.repository.ChatRoomMemberRepository;
import com.example.project.domain.chat.repository.ChatRoomRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public Long createPersonalChatRoom(Long userId, Long targetId) {
        // 1. 유저 정보 조회
        Users me = findUserById(userId);
        Users target = findUserById(targetId);

     // 이미 두 사람 사이에 개인 채팅방이 있는지 먼저 확인 - 팀장님 이거 제가 추가햇어여
        Optional<Long> existingRoomId = chatRoomMemberRepository.findPersonalRoomBetweenUsers(userId, targetId);
        
        // 만약 방이 이미 있다면, 새로 만들지 않고 기존 방 ID를 바로 반환
        if (existingRoomId.isPresent()) {
            return existingRoomId.get();
        }
        
        
        // 2. 채팅방 객체 생성 (방 자체 정보만!)
        ChatRoom chatRoom = ChatRoom.builder()
                .type(ChatRoomType.PERSONAL)
                .roomName(target.getNickname() + "님과의 대화") // 기본 방 이름 설정
                .build();
        chatRoomRepository.save(chatRoom);

        // 3. ChatRoomMember에 '나'와 '상대방'을 각각 저장 (이게 포인트!)
        // 나를 OWNER로 등록
        ChatRoomMember myMember = ChatRoomMember.builder()
                .user(me)
                .room(chatRoom)
                .role(MemberRole.OWNER)
                .build();
        chatRoomMemberRepository.save(myMember);

        // 상대방을 PARTICIPANT로 등록
        ChatRoomMember targetMember = ChatRoomMember.builder()
                .user(target)
                .room(chatRoom)
                .role(MemberRole.PARTICIPANT)
                .build();
        chatRoomMemberRepository.save(targetMember);

        return chatRoom.getRoomId();
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
    
    
    // 공통 로직: 사용자 조회
    private Users findUserById(Long userId) {
        return usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    // 공통 로직: 멤버 등록
    private void saveChatRoomMember(Users user, ChatRoom room, MemberRole role) {
        ChatRoomMember member = ChatRoomMember.builder()
                .user(user)
                .room(room)
                .role(role)
                .build();
        chatRoomMemberRepository.save(member);
    }
}
