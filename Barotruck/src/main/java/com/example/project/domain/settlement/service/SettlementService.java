package com.example.project.domain.settlement.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.settlement.domain.Settlement;
import com.example.project.domain.settlement.dto.SettlementRequest;
import com.example.project.domain.settlement.repository.SettlementRepository;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;

    /**
     * 1. 결제 준비 단계 (화주가 결제창을 열거나 요청했을 때)
     */
    public void initiateSettlement(SettlementRequest request, Users user) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 주문 시점의 스냅샷 가격 정보 가져오기
        var snapshot = order.getSnapshot();
        long baseTotal = snapshot.getBasePrice() + snapshot.getLaborFee() + 
                         snapshot.getPackagingPrice() + snapshot.getInsuranceFee();
        
        // 최종 결제 금액 계산
        long totalPrice = baseTotal - request.getCouponDiscount() - request.getLevelDiscount();

        Settlement settlement = Settlement.builder()
                .order(order)
                .user(user)
                .levelDiscount(request.getLevelDiscount())
                .couponDiscount(request.getCouponDiscount())
                .totalPrice(totalPrice)
                .feeRate(10L) // 기본 수수료율 10% 설정
                .status("READY") // 결제 대기 상태
                .feeDate(LocalDateTime.now())
                .build();

        // Order 엔티티에 정산 정보 연결 (편의 메서드 활용)
        order.setSettlement(settlement);
        settlementRepository.save(settlement);
    }

    /**
     * 2. 결제 완료 승인 (PG사 결제 성공 콜백 또는 최종 확정 시)
     */
    public void completeSettlement(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Settlement settlement = order.getSettlement();
        if (settlement == null) throw new IllegalStateException("정산 정보가 없습니다.");

        // 정산 상태 업데이트
        settlement.setStatus("COMPLETED");
        settlement.setFeeCompleteDate(LocalDateTime.now());

        // 오더 상태를 완료(COMPLETED)로 변경
        order.changeStatus("COMPLETED");
    }
}