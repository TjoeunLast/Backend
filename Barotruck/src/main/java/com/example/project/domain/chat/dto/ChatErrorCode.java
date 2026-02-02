package com.example.project.domain.chat.dto;

public enum ChatErrorCode {
    ROOM_NOT_FOUND("해당 채팅방을 찾을 수 없습니다."),
    ACCESS_DENIED("해당 채팅방에 접근 권한이 없습니다."),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.");

    private final String message;
    ChatErrorCode(String message) { this.message = message; }
    public String getMessage() { return message; }
}
