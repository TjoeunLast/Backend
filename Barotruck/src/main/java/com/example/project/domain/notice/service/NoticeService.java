package com.example.project.domain.notice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.notice.domain.Notice;
import com.example.project.domain.notice.dto.NoticeRequest;
import com.example.project.domain.notice.dto.NoticeResponse;
import com.example.project.domain.notice.repository.NoticeRepository;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;

    @Transactional
    public Long createNotice(NoticeRequest request, Users admin) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : "N")
                .admin(admin)
                .build();
        return noticeRepository.save(notice).getNoticeId();
    }

    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllWithAdminOrderByPinned().stream()
                .map(n -> NoticeResponse.builder()
                        .noticeId(n.getNoticeId())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .isPinned(n.getIsPinned())
                        // admin이 null일 경우를 대비한 방어 코드
                        .adminName(n.getAdminName() != null ? n.getAdminName() : "관리자")
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateNotice(Long id, NoticeRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다."));
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setIsPinned(request.getIsPinned());
    }

    @Transactional
    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}