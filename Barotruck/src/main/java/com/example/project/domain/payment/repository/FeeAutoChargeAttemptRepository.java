package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.FeeAutoChargeAttempt;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeAutoChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeeAutoChargeAttemptRepository extends JpaRepository<FeeAutoChargeAttempt, Long> {
    List<FeeAutoChargeAttempt> findTop20ByInvoiceIdOrderByAttemptIdDesc(Long invoiceId);

    List<FeeAutoChargeAttempt> findTop20ByShipperUserIdOrderByAttemptIdDesc(Long shipperUserId);

    List<FeeAutoChargeAttempt> findByInvoiceIdOrderByAttemptIdDesc(Long invoiceId, Pageable pageable);

    List<FeeAutoChargeAttempt> findByShipperUserIdOrderByAttemptIdDesc(Long shipperUserId, Pageable pageable);

    long countByShipperUserId(Long shipperUserId);

    long countByShipperUserIdAndStatus(Long shipperUserId, FeeAutoChargeStatus status);
}
