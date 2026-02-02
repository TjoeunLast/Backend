package com.example.project.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.project.global.exception.CustomException;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------
    // 1) 커스텀 예외 처리
    // -------------------------------
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code(e.getCode())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // -------------------------------
    // 2) RuntimeException 처리
    // -------------------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code("RUNTIME_EXCEPTION")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------
    // 3) IllegalStateException 처리
    // -------------------------------
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code("ILLEGAL_STATE")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------
    // 4) 예상 못한 모든 예외 처리
    // -------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 원인 파악: 서버 콘솔/로그에 실제 예외 출력 (INTERNAL_SERVER_ERROR 시 확인)
        log.error("INTERNAL_SERVER_ERROR: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message("서버 내부 오류가 발생했습니다.")
                .code("INTERNAL_SERVER_ERROR")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

   
//    EntityNotFoundException (404 Not Found)
//    의미: 요청한 데이터(예: 특정 ID의 지출 내역)가 데이터베이스에 존재하지 않을 때 발생합니다.
//    사용 예: 유저가 지운 내역을 다시 조회하려고 하거나, 잘못된 ID를 입력했을 때 "해당 내역을 찾을 수 없습니다"라는 메시지를 보냅니다.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

//    AccessDeniedException (403 Forbidden)
//    의미: 데이터는 존재하지만, 요청한 유저가 해당 데이터를 건드릴 권한이 없을 때 발생합니다.
//    사용 예: A라는 유저가 B라는 유저의 지출 내역을 삭제하려고 시도할 때 "권한이 없습니다"라고 차단합니다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }


    
    // -------------------------------
    // 에러 응답 공통 구조
    // -------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorResponse {
        private boolean success;
        private String message;
        private String code;
    }
}
