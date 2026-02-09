package com.example.project.domain.notice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeResponse {
    private Long noticeId;
    private String title;
    private String content;
    private String isPinned;
    private String adminName;
    private LocalDateTime createdAt;
}
