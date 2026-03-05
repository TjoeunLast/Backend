package com.example.project.domain.order.service;

import com.example.project.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderService orderService;

    // 매일 새벽 2시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 2 * * *")
    public void autoCancelUnassignedOrders() {
        log.info("미배정 오더 자동 취소 스케줄러 시작");
        orderService.cancelExpiredOrders();
        log.info("미배정 오더 자동 취소 스케줄러 종료");
    }
}