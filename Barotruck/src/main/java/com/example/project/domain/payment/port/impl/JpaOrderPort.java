package com.example.project.domain.payment.port.impl;

/**
 * JpaOrderPort
 *
 * 역할:
 * - 결제 도메인에서 필요한 "주문 관련 기능"을 JPA로 구현한 클래스
 * - OrderPort 인터페이스의 실제 실행 담당자
 *
 * 구조:
 * PaymentService → OrderPort → JpaOrderPort → OrderRepository → DB
 *
 * 즉,
 * 결제 서비스는 OrderRepository를 직접 모른다.
 * 이 클래스가 중간에서 DB 접근을 수행한다.
 */

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.payment.port.OrderPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaOrderPort implements OrderPort {

    /**
     * 실제 DB 접근을 담당하는 JPA Repository
     */
    private final OrderRepository orderRepository;


    /**
     * 결제에 반드시 필요한 주문 스냅샷 정보를 조회한다.
     *
     * 동작:
     * 1. 주문을 DB에서 조회
     * 2. snapshot 존재 여부 확인
     * 3. base + labor + packaging + insurance 합산
     * 4. OrderSnapshot DTO로 변환해서 반환
     *
     * 특징:
     * - 결제 도메인이 Order 엔티티 전체를 알 필요 없도록
     *   필요한 정보만 스냅샷 형태로 전달한다.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderPort.OrderSnapshot getRequiredSnapshot(Long orderId) {

        // 1️⃣ 주문 조회 (없으면 예외)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        // 2️⃣ 스냅샷 조회 (없으면 예외)
        var snap = order.getSnapshot();
        if (snap == null) {
            throw new IllegalStateException("order snapshot not found: " + orderId);
        }

        // 3️⃣ 금액 합산 (null 값 방지 처리)
        BigDecimal amount = BigDecimal.valueOf(nullSafe(snap.getBasePrice()))
                .add(BigDecimal.valueOf(nullSafe(snap.getLaborFee())))
                .add(BigDecimal.valueOf(nullSafe(snap.getPackagingPrice())))
                .add(BigDecimal.valueOf(nullSafe(snap.getInsuranceFee())));

        // 4️⃣ OrderSnapshot DTO 생성 후 반환
        return new OrderPort.OrderSnapshot(
                order.getOrderId(),
                order.getUser() != null ? order.getUser().getUserId() : null,
                order.getDriverNo(),
                amount,
                order.getStatus()
        );
    }


    /**
     * 주문 상태를 PAID로 변경
     * (결제 완료 시 호출)
     */
    @Override
    @Transactional
    public void setOrderPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        // 도메인 메서드를 통해 상태 변경
        order.changeStatus("PAID");
    }


    /**
     * 주문 상태를 CONFIRMED로 변경
     */
    @Override
    @Transactional
    public void setOrderConfirmed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        order.changeStatus("CONFIRMED");
    }


    /**
     * 주문 상태를 DISPUTED로 변경
     */
    @Override
    @Transactional
    public void setOrderDisputed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        order.changeStatus("DISPUTED");
    }


    /**
     * 금액 null 방지용 유틸
     * null이면 0 반환
     */
    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
