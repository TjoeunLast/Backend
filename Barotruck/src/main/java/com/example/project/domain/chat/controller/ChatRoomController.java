package com.example.project.domain.chat.controller;


import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.chat.dto.ChatHistoryResponse;
import com.example.project.domain.chat.dto.ChatRoomResponse;
import com.example.project.domain.chat.service.ChatRoomService;
import com.example.project.domain.chat.service.ChatService;
import com.example.project.member.domain.CustomUserDetails;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    
 // 1. 개인 채팅방 생성 (내 ID는 토큰에서, 상대 ID만 받음)
    @PostMapping("/room/personal/{targetId}") // 경로에 변수 추가
    public ResponseEntity<Long> createPersonalRoom(
            @AuthenticationPrincipal Users userDetails, 
            @PathVariable("targetId") Long targetId) { // @RequestBody 대신 @PathVariable 사용
        
        // 로그를 찍어 실제 값이 들어오는지 확인해보세요
        System.out.println("로그인 유저 ID: " + userDetails.getUserId());
        System.out.println("상대방 ID (PathVariable): " + targetId);
        
        Long roomId = this.chatRoomService.createPersonalChatRoom(userDetails.getUserId(), targetId);
        return ResponseEntity.ok(roomId);
    }


    /**
     * 3. 내가 참여 중인 채팅방 목록 조회
     */
    @GetMapping("/room")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
    		@AuthenticationPrincipal Users userDetails
    		) {
        return ResponseEntity.ok(chatService.getMyRooms(userDetails.getUserId()));
    }

    /**
     * 4. 채팅방 상세 및 과거 내역 조회 (페이징 적용)
     * GET /api/chat/room/{roomId}?userId=1&page=0&size=20
     * GET 형태로 http://localhost:8080/api/chat/room/1  이런식으로 보내면 됨 roomId 는 /rooms 에서 받은 데이터 가져와서 쓰면 됨 
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Users userDetails,
            @PageableDefault(size = 30) Pageable pageable) {
        // 방 입장 시 읽음 처리 로직 포함
        return ResponseEntity.ok(chatService.getChatHistory(roomId, userDetails.getUserId(), pageable));
    }

    /**
     * 5. 채팅방 나가기
     */
    @DeleteMapping("/room/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Users userDetails) {
        chatService.leaveRoom(roomId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
