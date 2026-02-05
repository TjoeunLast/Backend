package com.example.project.domain.notice.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.domain.notice.domain.Notice;
import com.example.project.domain.notice.dto.NoticeResponse;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 상단 고정글(Y)을 먼저 보여주고, 그 안에서 최신순 정렬
    List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();

	Collection<NoticeResponse> findAllWithAdminOrderByPinned();
}
