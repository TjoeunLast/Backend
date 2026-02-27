package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.FeePolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeePolicyConfigRepository extends JpaRepository<FeePolicyConfig, Long> {
    Optional<FeePolicyConfig> findTopByOrderByPolicyIdDesc();
}

