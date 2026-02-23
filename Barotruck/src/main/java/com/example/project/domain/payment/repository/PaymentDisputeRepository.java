package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.PaymentDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, Long> {
    Optional<PaymentDispute> findByOrderId(Long orderId);
}
