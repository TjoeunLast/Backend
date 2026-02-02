package com.example.project.domain.chat.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Slice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {

    private Long roomId;
    private List<ChatMessageResponse> messages;
    
    private int currentPage;
    private boolean hasNext; // 다음(더 과거의) 페이지 존재 여부

    /**
     * Slice 객체를 받아 DTO로 변환하는 정적 메서드
     */
    public static ChatHistoryResponse of(Long roomId, Slice<com.example.project.domain.chat.domain.ChatMessage> slice) {
        List<ChatMessageResponse> messageDtos = slice.getContent().stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .roomId(roomId)
                .messages(messageDtos)
                .currentPage(slice.getNumber())
                .hasNext(slice.hasNext())
                .build();
    }
}
