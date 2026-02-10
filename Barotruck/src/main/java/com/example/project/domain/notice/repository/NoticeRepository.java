package com.example.project.domain.notice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.domain.notice.domain.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 상단 고정글(Y)을 먼저 보여주고, 그 안에서 최신순 정렬
	@Query("SELECT n FROM Notice n JOIN FETCH n.admin ORDER BY n.isPinned DESC, n.createdAt DESC")
    List<Notice> findAllWithAdminOrderByIsPinnedDescCreatedAtDesc();

    // 상세 조회 (admin까지 한번에)
    @Query("SELECT n FROM Notice n JOIN FETCH n.admin WHERE n.noticeId = :id")
    Optional<Notice> findByIdWithAdmin(@Param("id") Long id);
}
