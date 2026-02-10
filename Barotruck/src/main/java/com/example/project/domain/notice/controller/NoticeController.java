package com.example.project.domain.notice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.notice.dto.NoticeRequest;
import com.example.project.domain.notice.dto.NoticeResponse;
import com.example.project.domain.notice.service.NoticeService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    // 공지사항 목록 조회 (누구나 가능)
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> list() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    // 추가: 공지사항 상세 조회 (누구나 가능)
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getNoticeDetail(id));
    }

    // 공지사항 작성 (관리자만)
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Long> create(
    		@RequestBody NoticeRequest request, 
    		@AuthenticationPrincipal Users admin) {
        return ResponseEntity.ok(noticeService.createNotice(request, admin));
    }

    // 공지사항 수정 (관리자만)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> update(
    		@PathVariable Long id, 
    		@RequestBody NoticeRequest request
    		) {
        noticeService.updateNotice(id, request);
        return ResponseEntity.ok().build();
    }

    // 공지사항 삭제 (관리자만)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> delete(
    		@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok().build();
    }
}