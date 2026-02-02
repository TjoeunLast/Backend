package com.example.project.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter // 값을 수정하기 위해 반드시 필요!
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long roomId;
    private Long senderId;
    private String content;
    private String type; // TEXT, IMAGE 등
}
