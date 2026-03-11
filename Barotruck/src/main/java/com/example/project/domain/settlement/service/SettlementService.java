package com.example.project.domain.settlement.service;

import com.example.project.domain.payment.domain.PaymentDispute;
import com.example.project.domain.payment.domain.TransportPaymentPricingSnapshot;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeReason;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentDisputeStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.TransportPaymentStatus;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.dto.SettlementRegionStatResponse;
import com.example.project.domain.settlement.dto.SettlementResponse;
import com.example.project.domain.settlement.dto.SettlementStatusSummaryResponse;
import com.example.project.domain.settlement.dto.SettlementSummaryResponse;
import com.example.project.domain.settlement.dto.UpdateSettlementStatusRequest;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UsersRepository usersRepository;
    private final TransportPaymentRepository transportPaymentRepository;
    private final PaymentDisputeRepository paymentDisputeRepository;
    private final DriverPayoutItemRepository driverPayoutItemRepository;

    public SettlementResponse updateSettlementStatus(
            Long orderId,
            UpdateSettlementStatusRequest request,
            Users currentUser
    ) {
        requireAuthenticated(currentUser);
        requireAdmin(currentUser);
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }

        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("settlement not found. orderId=" + orderId));
        TransportPayment payment = transportPaymentRepository.findByOrderId(orderId).orElse(null);
        PaymentDispute dispute = paymentDisputeRepository.findByOrderId(orderId).orElse(null);

        applySettlementStatusTransition(settlement, payment, dispute, request, currentUser);
        settlementRepository.save(settlement);
        return toResponse(settlement);
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlementForOrder(Long orderId, Users currentUser) {
        requireAuthenticated(currentUser);
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("settlement not found. orderId=" + orderId));
        validateReadPermission(settlement, currentUser);
        return toResponse(settlement);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getMySettlements(Users currentUser, String status) {
        requireAuthenticated(currentUser);

        List<Settlement> base;
        if (currentUser.getRole() == Role.SHIPPER) {
            base = settlementRepository.findByUser_UserIdOrderByFeeDateDesc(currentUser.getUserId());
        } else if (currentUser.getRole() == Role.DRIVER) {
            base = settlementRepository.findByDriverUserIdOrderByFeeDateDesc(currentUser.getUserId());
        } else if (currentUser.getRole() == Role.ADMIN) {
            throw new IllegalStateException("admin must use admin settlement list api");
        } else {
            throw new IllegalStateException("unsupported role for settlement view");
        }

        return filterAndMapSettlements(base, status);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getAdminSettlements(Users currentUser, String status) {
        requireAuthenticated(currentUser);
        requireAdmin(currentUser);
        return filterAndMapSettlements(settlementRepository.findAll(), status);
    }

    @Transactional(readOnly = true)
    public SettlementSummaryResponse getSettlementSummary(LocalDateTime start, LocalDateTime end) {
        Object[] row = settlementRepository.getSettlementSummary(start, end);
        if (row == null || row.length < 4) {
            return SettlementSummaryResponse.builder()
                    .totalAmount(0L)
                    .platformRevenue(0L)
                    .totalDiscount(0L)
                    .count(0L)
                    .build();
        }
        return SettlementSummaryResponse.builder()
                .totalAmount(toLong(row[0]))
                .platformRevenue(toLong(row[1]))
                .totalDiscount(toLong(row[2]))
                .count(toLong(row[3]))
                .build();
    }

    @Transactional(readOnly = true)
    public SettlementStatusSummaryResponse getSettlementStatusSummary() {
        Object[] row = settlementRepository.getSettlementStatusSummary(SettlementStatus.COMPLETED);
        if (row == null || row.length < 6) {
            return SettlementStatusSummaryResponse.builder()
                    .totalAmount(0L)
                    .pendingAmount(0L)
                    .completedAmount(0L)
                    .totalCount(0L)
                    .pendingCount(0L)
                    .completedCount(0L)
                    .build();
        }

        return SettlementStatusSummaryResponse.builder()
                .totalAmount(toLong(row[0]))
                .pendingAmount(toLong(row[1]))
                .completedAmount(toLong(row[2]))
                .totalCount(toLong(row[3]))
                .pendingCount(toLong(row[4]))
                .completedCount(toLong(row[5]))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SettlementRegionStatResponse> getSettlementRegionStats(LocalDateTime start, LocalDateTime end) {
        return settlementRepository.getSettlementStatsByRegion(start, end).stream()
                .map(row -> SettlementRegionStatResponse.builder()
                        .province(row[0] == null ? null : String.valueOf(row[0]))
                        .totalAmount(toLong(row[1]))
                        .count(toLong(row[2]))
                        .build())
                .toList();
    }

    private void applySettlementStatusTransition(
            Settlement settlement,
            TransportPayment payment,
            PaymentDispute dispute,
            UpdateSettlementStatusRequest request,
            Users currentUser
    ) {
        SettlementStatus nextStatus = request.getStatus();
        String adminMemo = normalize(request.getAdminMemo());

        if (settlement.getFeeDate() == null) {
            settlement.setFeeDate(LocalDateTime.now());
        }
        settlement.setField5(adminMemo);

        switch (nextStatus) {
            case READY -> {
                settlement.setStatus(SettlementStatus.READY);
                settlement.setFeeCompleteDate(null);
                releaseSettlementHold(payment, dispute, currentUser, adminMemo);
            }
            case WAIT -> {
                settlement.setStatus(SettlementStatus.WAIT);
                settlement.setFeeCompleteDate(null);
                holdSettlement(payment, dispute, currentUser, adminMemo);
            }
            case COMPLETED -> {
                settlement.setStatus(SettlementStatus.COMPLETED);
                settlement.setFeeCompleteDate(LocalDateTime.now());
                resolveSettlementForCompletion(payment, dispute, currentUser, adminMemo);
            }
        }
    }

    private void holdSettlement(
            TransportPayment payment,
            PaymentDispute dispute,
            Users currentUser,
            String adminMemo
    ) {
        if (payment == null) {
            return;
        }
        if (payment.getStatus() == TransportPaymentStatus.CANCELLED) {
            throw new IllegalStateException("cannot hold cancelled payment");
        }

        payment.updateStatus(TransportPaymentStatus.ADMIN_HOLD);
        transportPaymentRepository.save(payment);
        upsertAdminDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_HOLD, adminMemo);
    }

    private void releaseSettlementHold(
            TransportPayment payment,
            PaymentDispute dispute,
            Users currentUser,
            String adminMemo
    ) {
        if (payment == null) {
            return;
        }

        if (isDisputeBackedStatus(payment.getStatus()) || dispute != null) {
            forceConfirmPayment(payment);
            transportPaymentRepository.save(payment);
            upsertAdminDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED, adminMemo);
        }
    }

    private void resolveSettlementForCompletion(
            TransportPayment payment,
            PaymentDispute dispute,
            Users currentUser,
            String adminMemo
    ) {
        if (payment == null) {
            return;
        }

        if (isDisputeBackedStatus(payment.getStatus()) || dispute != null) {
            forceConfirmPayment(payment);
            transportPaymentRepository.save(payment);
            upsertAdminDispute(payment, dispute, currentUser, PaymentDisputeStatus.ADMIN_FORCE_CONFIRMED, adminMemo);
        }
    }

    private void forceConfirmPayment(TransportPayment payment) {
        if (payment.getConfirmedAt() == null) {
            payment.confirm(LocalDateTime.now());
        }
        payment.updateStatus(TransportPaymentStatus.ADMIN_FORCE_CONFIRMED);
    }

    private void upsertAdminDispute(
            TransportPayment payment,
            PaymentDispute dispute,
            Users currentUser,
            PaymentDisputeStatus nextStatus,
            String adminMemo
    ) {
        if (payment == null || payment.getPaymentId() == null) {
            return;
        }

        PaymentDispute target = dispute;
        if (target == null) {
            Long requesterUserId = payment.getDriverUserId() != null
                    ? payment.getDriverUserId()
                    : currentUser.getUserId();
            target = PaymentDispute.create(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    requesterUserId,
                    currentUser.getUserId(),
                    PaymentDisputeReason.OTHER,
                    buildAdminDisputeDescription(nextStatus, adminMemo),
                    null
            );
        }
        target.updateStatus(nextStatus, adminMemo);
        paymentDisputeRepository.save(target);
    }

    private String buildAdminDisputeDescription(
            PaymentDisputeStatus nextStatus,
            String adminMemo
    ) {
        if (adminMemo != null && !adminMemo.isBlank()) {
            return adminMemo;
        }
        return "ADMIN settlement status update: " + nextStatus.name();
    }

    private boolean isDisputeBackedStatus(TransportPaymentStatus status) {
        if (status == null) {
            return false;
        }
        return status == TransportPaymentStatus.DISPUTED
                || status == TransportPaymentStatus.ADMIN_HOLD
                || status == TransportPaymentStatus.ADMIN_REJECTED;
    }

    private void validateReadPermission(Settlement settlement, Users currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Long currentUserId = currentUser.getUserId();
        if (currentUser.getRole() == Role.SHIPPER) {
            Long ownerId = settlement.getUser() != null ? settlement.getUser().getUserId() : null;
            if (ownerId != null && ownerId.equals(currentUserId)) {
                return;
            }
        }

        if (currentUser.getRole() == Role.DRIVER) {
            Long driverUserId = settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null;
            if (driverUserId != null && driverUserId.equals(currentUserId)) {
                return;
            }
        }

        throw new IllegalStateException("forbidden settlement access");
    }

    private void requireAuthenticated(Users user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private void requireAdmin(Users user) {
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("only admin can access settlement admin api");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private SettlementStatus parseSettlementStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SettlementStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid settlement status: " + status);
        }
    }

    private List<SettlementResponse> filterAndMapSettlements(List<Settlement> base, String status) {
        if (status == null || status.isBlank()) {
            return base.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        SettlementStatus normalizedStatus = parseSettlementStatus(status);
        return base.stream()
                .filter(settlement -> settlement.getStatus() == normalizedStatus)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SettlementResponse toResponse(Settlement settlement) {
    	// 차주 ID(driverNo) 추출
        Long driverNo = settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null;
        TransportPayment transportPayment = settlement.getOrder() == null
                ? null
                : transportPaymentRepository.findByOrderId(settlement.getOrder().getOrderId()).orElse(null);
        TransportPaymentPricingSnapshot pricingSnapshot = TransportPaymentPricingSnapshot.from(transportPayment);
        DriverPayoutItem payoutItem = settlement.getOrder() == null
                ? null
                : driverPayoutItemRepository.findByOrderId(settlement.getOrder().getOrderId()).orElse(null);

        // 2. 차주 이름 조회 로직 추가
        String driverName = "미지정";
        String bankName = null;
        String accountNum = null;

        String shipperName = "미지정";
        String bizNumber = "정보 없음";
        
        // 람다 제약 문제를 피하기 위해 Optional을 직접 처리합니다.
        if (driverNo != null) {
            Users driverUser = usersRepository.findById(driverNo).orElse(null);
            if (driverUser != null) {
                driverName = driverUser.getName();
                // Driver 엔티티와 1:1 관계인 경우 정보를 가져옵니다.
                if (driverUser.getDriver() != null) {
                    bankName = driverUser.getDriver().getBankName();
                    accountNum = driverUser.getDriver().getAccountNum();
                }
            }
        }
        
        Users shipperUser = settlement.getUser(); // 정산 객체에 연결된 화주 유저
        if (shipperUser != null && shipperUser.getShipper() != null) {
            // Shipper 엔티티에서 회사명과 사업자 번호를 가져옵니다.
            shipperName = shipperUser.getShipper().getCompanyName(); 
            bizNumber = shipperUser.getShipper().getBizRegNum();
        }

        Long baseAmount = firstNonNull(
                settlement.getBaseAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.baseAmount()) : null
        );
        BigDecimal shipperFeeRate = firstNonNull(
                settlement.getShipperFeeRateSnapshot(),
                pricingSnapshot != null ? pricingSnapshot.shipperFeeRate() : null
        );
        Long shipperFeeAmount = firstNonNull(
                settlement.getShipperFeeAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.shipperFeeAmount()) : null
        );
        Boolean shipperPromoApplied = firstNonNull(
                settlement.getShipperPromoApplied(),
                pricingSnapshot != null ? pricingSnapshot.shipperPromoApplied() : null
        );
        Long shipperChargeAmount = firstNonNull(
                settlement.getShipperChargeAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.shipperChargeAmount()) : null,
                settlement.getTotalPrice()
        );
        BigDecimal driverFeeRate = firstNonNull(
                settlement.getDriverFeeRateSnapshot(),
                pricingSnapshot != null ? pricingSnapshot.driverFeeRate() : null
        );
        Long driverFeeAmount = firstNonNull(
                settlement.getDriverFeeAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.driverFeeAmount()) : null
        );
        Boolean driverPromoApplied = firstNonNull(
                settlement.getDriverPromoApplied(),
                pricingSnapshot != null ? pricingSnapshot.driverPromoApplied() : null
        );
        Long driverPayoutAmount = firstNonNull(
                settlement.getDriverPayoutAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.driverPayoutAmount()) : null,
                transportPayment != null ? toLong(transportPayment.getNetAmountSnapshot()) : null
        );
        BigDecimal tossFeeRate = firstNonNull(
                settlement.getTossFeeRateSnapshot(),
                pricingSnapshot != null ? pricingSnapshot.tossFeeRate() : null
        );
        Long tossFeeAmount = firstNonNull(
                settlement.getTossFeeAmountSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.tossFeeAmount()) : null
        );
        Long platformGrossRevenue = firstNonNull(
                settlement.getPlatformGrossRevenueSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.platformGrossRevenue()) : null
        );
        Long platformNetRevenue = firstNonNull(
                settlement.getPlatformNetRevenueSnapshot(),
                pricingSnapshot != null ? toLong(pricingSnapshot.platformNetRevenue()) : null,
                settlement.calculatePlatformRevenue()
        );
        Boolean negativeMargin = platformNetRevenue != null && platformNetRevenue < 0;
        Long feePolicyId = firstNonNull(
                settlement.getFeePolicyIdSnapshot(),
                pricingSnapshot != null ? pricingSnapshot.feePolicyId() : null
        );
        LocalDateTime feePolicyAppliedAt = firstNonNull(
                settlement.getFeePolicyAppliedAtSnapshot(),
                pricingSnapshot != null ? pricingSnapshot.feePolicyAppliedAt() : null
        );
        
        return SettlementResponse.builder()
                .settlementId(settlement.getId())
                .orderId(settlement.getOrder() != null ? settlement.getOrder().getOrderId() : null)
                .shipperUserId(settlement.getUser() != null ? settlement.getUser().getUserId() : null)
                .driverUserId(settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null)
                .driverName(driverName)
                .bankName(bankName)       // 빌더에 추가
                .accountNum(accountNum)   // 빌더에 추가
                .shipperName(shipperName)
                .bizNumber(bizNumber)
                .orderStatus(settlement.getOrder() != null ? settlement.getOrder().getStatus() : null)
                .paymentId(transportPayment != null ? transportPayment.getPaymentId() : null)
                .paymentMethod(transportPayment != null && transportPayment.getMethod() != null ? transportPayment.getMethod().name() : null)
                .paymentTiming(transportPayment != null && transportPayment.getPaymentTiming() != null ? transportPayment.getPaymentTiming().name() : null)
                .paymentStatus(transportPayment != null && transportPayment.getStatus() != null ? transportPayment.getStatus().name() : null)
                .paymentAmount(transportPayment != null ? toLong(transportPayment.getAmount()) : null)
                .paymentFeeAmount(transportPayment != null ? toLong(transportPayment.getFeeAmountSnapshot()) : null)
                .paymentNetAmount(transportPayment != null ? toLong(transportPayment.getNetAmountSnapshot()) : null)
                .baseAmount(baseAmount)
                .shipperFeeRate(shipperFeeRate)
                .shipperFeeAmount(shipperFeeAmount)
                .shipperPromoApplied(shipperPromoApplied)
                .shipperChargeAmount(shipperChargeAmount)
                .driverFeeRate(driverFeeRate)
                .driverFeeAmount(driverFeeAmount)
                .driverPromoApplied(driverPromoApplied)
                .driverPayoutAmount(driverPayoutAmount)
                .tossFeeRate(tossFeeRate)
                .tossFeeAmount(tossFeeAmount)
                .platformGrossRevenue(platformGrossRevenue)
                .platformNetRevenue(platformNetRevenue)
                .negativeMargin(negativeMargin)
                .feePolicyId(feePolicyId)
                .feePolicyAppliedAt(feePolicyAppliedAt)
                .pgTid(transportPayment != null ? transportPayment.getPgTid() : null)
                .proofUrl(transportPayment != null ? transportPayment.getProofUrl() : null)
                .paidAt(transportPayment != null ? transportPayment.getPaidAt() : null)
                .confirmedAt(transportPayment != null ? transportPayment.getConfirmedAt() : null)
                .levelDiscount(settlement.getLevelDiscount())
                .couponDiscount(settlement.getCouponDiscount())
                .totalPrice(settlement.getTotalPrice())
                .feeRate(settlement.getFeeRate())
                .status(settlement.getStatus() == null ? null : settlement.getStatus().name())
                .payoutStatus(payoutItem != null && payoutItem.getStatus() != null ? payoutItem.getStatus().name() : null)
                .payoutFailureReason(payoutItem != null ? payoutItem.getFailureReason() : null)
                .payoutRef(payoutItem != null ? payoutItem.getPayoutRef() : null)
                .payoutRequestedAt(payoutItem != null ? payoutItem.getRequestedAt() : null)
                .payoutCompletedAt(payoutItem != null ? payoutItem.getCompletedAt() : null)
                .feeDate(settlement.getFeeDate())
                .feeCompleteDate(settlement.getFeeCompleteDate())
                .build();
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
