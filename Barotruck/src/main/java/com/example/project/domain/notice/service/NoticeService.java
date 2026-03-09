package com.example.project.domain.notice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.notice.domain.Notice;
import com.example.project.domain.notice.dto.NoticeRequest;
import com.example.project.domain.notice.dto.NoticeResponse;
import com.example.project.domain.notice.repository.NoticeRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UsersRepository usersRepository;
    private final NotificationService notificationService;

    @Transactional
    public Long createNotice(NoticeRequest request, Users admin) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : "N")
                .admin(admin)
                .build();
        Notice saved = noticeRepository.save(notice);
        usersRepository.findAll(Sort.by(Sort.Direction.ASC, "userId"))
                .stream()
                .filter(user -> admin == null || !user.getUserId().equals(admin.getUserId()))
                .forEach(user -> sendNoticeNotificationSafely(
                        user,
                        "새 공지 등록",
                        request.getTitle(),
                        saved.getNoticeId()
                ));
        return saved.getNoticeId();
    }

    public List<NoticeResponse> getAllNotices() {
        // 리포지토리 메서드명을 앞서 수정한 이름으로 일치시켜야 합니다.
        return noticeRepository.findAllWithAdminOrderByIsPinnedDescCreatedAtDesc().stream()
                .map(n -> NoticeResponse.builder()
                        .noticeId(n.getNoticeId())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .isPinned(n.getIsPinned())
                        // admin 객체에 접근해서 이름을 가져와야 함 (Null 체크 포함)
                        .adminName(n.getAdmin() != null ? n.getAdmin().getNickname() : "관리자")
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

    // 공지 상세 조회
    public NoticeResponse getNoticeDetail(Long id) {
        Notice n = noticeRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다."));

        return NoticeResponse.builder()
                .noticeId(n.getNoticeId())
                .title(n.getTitle())
                .content(n.getContent())
                .isPinned(n.getIsPinned())
                .adminName(n.getAdmin() != null ? n.getAdmin().getNickname() : "관리자")
                .createdAt(n.getCreatedAt())
                .build();
    }

    private void sendNoticeNotificationSafely(Users recipient, String title, String body, Long targetId) {
        if (recipient == null) {
            return;
        }
        try {
            notificationService.sendNotification(recipient, "NOTICE", title, body, targetId);
        } catch (Exception e) {
            System.out.println("공지 알림 발송 중 예외 발생: " + e.getMessage());
        }
    }
}
