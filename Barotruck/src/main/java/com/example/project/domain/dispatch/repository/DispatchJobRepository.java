package com.example.project.domain.dispatch.repository;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DispatchJobRepository extends JpaRepository<DispatchJob, Long> {

    Optional<DispatchJob> findFirstByOrderIdAndActiveTrueOrderByStartedAtDesc(Long orderId);

    List<DispatchJob> findTop50ByOrderByStartedAtDesc();

    List<DispatchJob> findByStatusInAndExpiresAtBeforeAndActiveTrue(Collection<DispatchJobStatus> statuses, LocalDateTime threshold);

    List<DispatchJob> findByOrderIdOrderByStartedAtDesc(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from DispatchJob j where j.dispatchJobId = :jobId")
    Optional<DispatchJob> findByIdForUpdate(@Param("jobId") Long jobId);
}
