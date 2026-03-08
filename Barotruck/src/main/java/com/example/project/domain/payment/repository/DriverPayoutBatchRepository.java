package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DriverPayoutBatchRepository extends JpaRepository<DriverPayoutBatch, Long> {
    Optional<DriverPayoutBatch> findByBatchDate(LocalDate batchDate);
}
