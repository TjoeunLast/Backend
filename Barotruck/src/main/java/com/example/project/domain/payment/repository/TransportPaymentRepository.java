package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.TransportPaymentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransportPaymentRepository extends JpaRepository<TransportPayment, Long> {

    Optional<TransportPayment> findByOrderId(Long orderId);

    long countByShipperUserIdAndStatusIn(Long shipperUserId, List<TransportPaymentStatus> statuses);

    List<TransportPayment> findAllByStatusIn(List<TransportPaymentStatus> statuses);

    @Query("select distinct t.shipperUserId from TransportPayment t where t.paidAt >= :from and t.paidAt < :to")
    List<Long> findDistinctShipperUserIdByPaidAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

