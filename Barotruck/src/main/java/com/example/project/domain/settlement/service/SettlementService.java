package com.example.project.domain.settlement.service;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.domain.SettlementStatus;
import com.example.project.domain.settlement.dto.SettlementRegionStatResponse;
import com.example.project.domain.settlement.dto.SettlementRequest;
import com.example.project.domain.settlement.dto.SettlementResponse;
import com.example.project.domain.settlement.dto.SettlementStatusSummaryResponse;
import com.example.project.domain.settlement.dto.SettlementSummaryResponse;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private static final long DEFAULT_FEE_RATE = 10L;

    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final UsersRepository usersRepository; // 1. 추가: 유저 조회를 위해 필요

    /**
     * 정산 데이터 초기 생성/갱신
     * - 화주 또는 관리자만 호출 가능
     */
    public void initiateSettlement(SettlementRequest request, Users user) {
        requireAuthenticated(user);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found"));

        validateOwnerOrAdmin(order, user);

        OrderSnapshot snapshot = order.getSnapshot();
        if (snapshot == null) {
            throw new IllegalStateException("order snapshot not found");
        }

        long cargoPrice =
                nullSafe(snapshot.getBasePrice())
                        + nullSafe(snapshot.getLaborFee())
                        + nullSafe(snapshot.getPackagingPrice())
                        + nullSafe(snapshot.getInsuranceFee());

        long couponDiscount = nullSafe(request.getCouponDiscount());
        long levelDiscount = nullSafe(request.getLevelDiscount());
        long platformFee = (cargoPrice * DEFAULT_FEE_RATE) / 100;
        long totalPrice = Math.max(cargoPrice + platformFee - couponDiscount - levelDiscount, 0L);

        Settlement settlement = settlementRepository.findByOrderId(order.getOrderId())
                .orElseGet(() -> Settlement.builder()
                        .order(order)
                        .user(user)
                        .build());

        settlement.setUser(user);
        settlement.setLevelDiscount(levelDiscount);
        settlement.setCouponDiscount(couponDiscount);
        settlement.setTotalPrice(totalPrice);
        settlement.setFeeRate(DEFAULT_FEE_RATE);
        settlement.setStatus(SettlementStatus.READY);
        if (settlement.getFeeDate() == null) {
            settlement.setFeeDate(LocalDateTime.now());
        }

        order.setSettlement(settlement);
        settlementRepository.save(settlement);
    }

    /**
     * 레거시 호환용 완료 처리(호출자 권한 검증 없음)
     */
    public void completeSettlement(Long orderId) {
        completeSettlementInternal(orderId, null);
    }

    /**
     * 권한 검증 포함 완료 처리
     */
    public SettlementResponse completeSettlementByUser(Long orderId, Users currentUser) {
        return toResponse(completeSettlementInternal(orderId, currentUser));
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
        if (currentUser.getRole() == Role.ADMIN) {
            base = settlementRepository.findAll();
        } else if (currentUser.getRole() == Role.SHIPPER) {
            base = settlementRepository.findByUser_UserIdOrderByFeeDateDesc(currentUser.getUserId());
        } else if (currentUser.getRole() == Role.DRIVER) {
            base = settlementRepository.findByDriverUserIdOrderByFeeDateDesc(currentUser.getUserId());
        } else {
            throw new IllegalStateException("unsupported role for settlement view");
        }

        if (status == null || status.isBlank()) {
            return base.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        SettlementStatus normalizedStatus = parseSettlementStatus(status);
        return base.stream()
                .filter(s -> s.getStatus() == normalizedStatus)
                .map(this::toResponse)
                .collect(Collectors.toList());
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

    private Settlement completeSettlementInternal(Long orderId, Users currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found. orderId=" + orderId));

        if (currentUser != null) {
            requireAuthenticated(currentUser);
            validateOwnerOrAdmin(order, currentUser);
        }

        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("settlement not found. orderId=" + orderId));

        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setFeeCompleteDate(LocalDateTime.now());

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            order.changeStatus("COMPLETED");
        }

        return settlementRepository.save(settlement);
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

    private void validateOwnerOrAdmin(Order order, Users currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        Long ownerId = order.getUser() != null ? order.getUser().getUserId() : null;
        if (currentUser.getRole() == Role.SHIPPER && ownerId != null && ownerId.equals(currentUser.getUserId())) {
            return;
        }
        throw new IllegalStateException("only owner shipper or admin can handle settlement");
    }

    private void requireAuthenticated(Users user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalStateException("authentication required");
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
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

    private SettlementResponse toResponse(Settlement settlement) {
    	// 차주 ID(driverNo) 추출
        Long driverNo = settlement.getOrder() != null ? settlement.getOrder().getDriverNo() : null;
        
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
                .levelDiscount(settlement.getLevelDiscount())
                .couponDiscount(settlement.getCouponDiscount())
                .totalPrice(settlement.getTotalPrice())
                .feeRate(settlement.getFeeRate())
                .status(settlement.getStatus() == null ? null : settlement.getStatus().name())
                .feeDate(settlement.getFeeDate())
                .feeCompleteDate(settlement.getFeeCompleteDate())
                .build();
    }
}
