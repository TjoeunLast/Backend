package com.example.project.domain.payment.repository;

import com.example.project.domain.payment.domain.ShipperBillingAgreement;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.BillingAgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipperBillingAgreementRepository extends JpaRepository<ShipperBillingAgreement, Long> {
    Optional<ShipperBillingAgreement> findTopByShipperUserIdOrderByAgreementIdDesc(Long shipperUserId);

    Optional<ShipperBillingAgreement> findTopByShipperUserIdAndStatusOrderByAgreementIdDesc(
            Long shipperUserId,
            BillingAgreementStatus status
    );

    Optional<ShipperBillingAgreement> findByBillingKey(String billingKey);

    Optional<ShipperBillingAgreement> findByCustomerKey(String customerKey);

    List<ShipperBillingAgreement> findAllByShipperUserIdOrderByAgreementIdDesc(Long shipperUserId);
}
