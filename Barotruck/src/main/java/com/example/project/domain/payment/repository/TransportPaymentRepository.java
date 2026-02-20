package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.TransportPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransportPaymentRepository extends JpaRepository<TransportPayment, Long> {

    Optional<TransportPayment> findByOrderId(Long orderId);

    long countByShipperUserIdAndStatusIn(Long shipperUserId, List<TransportPaymentStatus> statuses);
}
