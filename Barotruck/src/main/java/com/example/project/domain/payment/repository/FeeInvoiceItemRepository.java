// src/main/java/com/example/project/domain/payment/repository/FeeInvoiceItemRepository.java
package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.FeeInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeInvoiceItemRepository extends JpaRepository<FeeInvoiceItem, Long> {

    List<FeeInvoiceItem> findAllByInvoiceId(Long invoiceId);

    boolean existsByInvoiceIdAndOrderId(Long invoiceId, Long orderId);
}
