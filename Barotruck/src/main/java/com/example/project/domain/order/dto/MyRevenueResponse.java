package com.example.project.domain.order.dto;


import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyRevenueResponse {
    private long totalAmount;    // 총 받는 금액 (이번 달 전체 합계)
    private long receivedAmount; // 현재 받은 금액 (정산 완료 건)
    private long pendingAmount;  // 앞으로 받을 금액 (운행 완료/정산 대기 건)
    
    private List<OrderResponse> orders; // 이번 달 상세 오더 목록
}
