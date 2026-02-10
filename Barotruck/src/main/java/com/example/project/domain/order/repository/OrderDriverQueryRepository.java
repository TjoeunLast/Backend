package com.example.project.domain.order.repository;

import com.example.project.domain.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderDriverQueryRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByDriverNo(@Param("driverNo") Long driverNo);
}
