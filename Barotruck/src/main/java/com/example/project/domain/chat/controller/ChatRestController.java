package com.example.project.domain.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.chat.dto.ChatMessageResponse;
import com.example.project.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private final ChatService chatService;

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }
}
