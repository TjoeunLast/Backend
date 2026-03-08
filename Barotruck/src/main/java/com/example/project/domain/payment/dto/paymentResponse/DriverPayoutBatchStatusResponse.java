package com.example.project.domain.payment.dto.paymentResponse;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DriverPayoutBatchStatusResponse(
        Long batchId,
        LocalDate batchDate,
        PayoutStatus status,
        Long totalItems,
        Long failedItems,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String failureReason
) {
    public static DriverPayoutBatchStatusResponse of(
            DriverPayoutBatch batch,
            long totalItems,
            long failedItems
    ) {
        return DriverPayoutBatchStatusResponse.builder()
                .batchId(batch.getBatchId())
                .batchDate(batch.getBatchDate())
                .status(batch.getStatus())
                .totalItems(totalItems)
                .failedItems(failedItems)
                .requestedAt(batch.getRequestedAt())
                .completedAt(batch.getCompletedAt())
                .failureReason(batch.getFailureReason())
                .build();
    }
}
