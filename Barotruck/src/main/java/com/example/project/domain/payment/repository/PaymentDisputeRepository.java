package com.example.project.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.domain.payment.domain.PaymentDispute;

public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, Long> {
    Optional<PaymentDispute> findByOrderId(Long orderId);
}
