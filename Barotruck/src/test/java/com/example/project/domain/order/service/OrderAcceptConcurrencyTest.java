package com.example.project.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.dispatch.service.availability.DispatchAvailabilityService;
import com.example.project.domain.dispatch.service.core.DispatchOrchestratorService;
import com.example.project.domain.notification.service.NotificationService;
import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.OrderRequest;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.global.image.S3ImageService;
import com.example.project.global.neighborhood.NeighborhoodService;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;

/**
 * 바로배차 주문을 여러 기사가 동시에 수락할 때 한 명만 배차되는지 검증한다.
 * 실제 OrderService + JPA(@Version 낙관적 락)를 H2 위에서 돌리고, 외부 연동 서비스만 mock 처리한다.
 * 각 스레드가 독립 트랜잭션으로 커밋해야 하므로 테스트 자체 트랜잭션은 끈다.
 */
@DataJpaTest
@Import(OrderService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderAcceptConcurrencyTest {

    private static final int DRIVER_COUNT = 50;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private DriverOrderRecommendationService driverOrderRecommendationService;

    @MockBean
    private NeighborhoodService neighborhoodService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private TransportPaymentService transportPaymentService;

    @MockBean
    private S3ImageService s3ImageService;

    @MockBean
    private DispatchOrchestratorService dispatchOrchestratorService;

    @MockBean
    private DispatchAvailabilityService dispatchAvailabilityService;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        usersRepository.deleteAll();
    }

    @Test
    void 바로배차_동시_수락은_한_명만_성공한다() throws Exception {
        Long orderId = saveInstantOrder();

        Queue<Long> acceptedDrivers = new ConcurrentLinkedQueue<>();
        Queue<Throwable> rejections = new ConcurrentLinkedQueue<>();
        CountDownLatch ready = new CountDownLatch(DRIVER_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(DRIVER_COUNT);

        for (int i = 1; i <= DRIVER_COUNT; i++) {
            long driverNo = 1000L + i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.acceptOrder(orderId, driverNo);
                    acceptedDrivers.add(driverNo);
                } catch (Throwable e) {
                    rejections.add(e);
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        printSummary(acceptedDrivers, rejections);

        assertThat(acceptedDrivers).hasSize(1);
        assertThat(rejections).hasSize(DRIVER_COUNT - 1);

        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("ACCEPTED");
        assertThat(saved.getDriverNo()).isEqualTo(acceptedDrivers.peek());
    }

    private Long saveInstantOrder() {
        Users shipper = usersRepository.save(Users.builder()
                .name("화주")
                .email("shipper@test.local")
                .role(Role.SHIPPER)
                .build());

        OrderRequest request = OrderRequest.builder()
                .startAddr("서울특별시 강남구")
                .endAddr("경기도 오산시")
                .instant(true)
                .build();

        return orderRepository.save(Order.createOrder(shipper, request)).getOrderId();
    }

    private void printSummary(Queue<Long> acceptedDrivers, Queue<Throwable> rejections) {
        System.out.printf("[동시 수락] 요청 %d건 / 성공 %d건 / 거절 %d건%n",
                DRIVER_COUNT, acceptedDrivers.size(), rejections.size());
        rejections.stream()
                .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
                .map(line -> line.length() > 120 ? line.substring(0, 120) : line)
                .collect(java.util.stream.Collectors.groupingBy(line -> line,
                        java.util.stream.Collectors.counting()))
                .forEach((line, count) -> System.out.printf("  - %d건 %s%n", count, line));
    }
}
