package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.paymentEnum.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverPayoutItemRepository extends JpaRepository<DriverPayoutItem, Long> {
    boolean existsByOrderId(Long orderId);

    List<DriverPayoutItem> findAllByStatusIn(List<PayoutStatus> statuses);

    long countByBatch_BatchIdAndStatus(Long batchId, PayoutStatus status);
}

