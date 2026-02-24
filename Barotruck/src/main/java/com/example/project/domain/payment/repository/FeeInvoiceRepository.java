package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.paymentEnum.FeeInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, Long> {

    Optional<FeeInvoice> findByShipperUserIdAndPeriod(Long shipperUserId, String period);

    List<FeeInvoice> findAllByShipperUserIdOrderByPeriodDesc(Long shipperUserId);

    List<FeeInvoice> findAllByStatus(FeeInvoiceStatus status);
}

