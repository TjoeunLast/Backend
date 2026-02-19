package com.example.project.domain.order.service.orderService;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.domain.embedded.OrderSnapshot;
import com.example.project.domain.order.dto.orderRequest.OrderSearchRequest;
import com.example.project.domain.order.dto.orderResponse.OrderListResponse;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.order.repository.OrderSearchSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public Page<OrderListResponse> search(OrderSearchRequest f) {
        if (f == null) f = new OrderSearchRequest();

        Sort sort = toSort(f.getSort());

        PageRequest pageable = PageRequest.of(
                safePage(f.getPage()),
                safeSize(f.getSize()),
                sort
        );

        var spec = OrderSearchSpecifications.byFilter(f);
        Page<Order> page = orderRepository.findAll(spec, pageable);

        boolean ai = "RECOMMEND".equalsIgnoreCase(nvl(f.getTab()))
                || "RECOMMEND".equalsIgnoreCase(nvl(f.getSort()));

        return page.map(o -> {
            OrderSnapshot s = o.getSnapshot(); // snapshot이 null일 가능성 있으면 null 방어 추가
            return new OrderListResponse(
                    o.getOrderId(),
                    s != null ? s.getStartPlace() : null,
                    s != null ? s.getEndPlace() : null,
                    o.getDistance(),
                    o.getSettlement() != null ? o.getSettlement().getTotalPrice() : null,
                    s != null ? s.getStartSchedule() : null,
                    s != null ? s.getReqCarType() : null,
                    s != null ? s.getReqTonnage() : null,
                    s != null ? s.getWorkType() : null,
                    ai,
                    s != null ? s.getDriveMode() : null,
                    o.getStatus()
            );
        });
    }

    private Sort toSort(String sort) {
        String s = (sort == null) ? "" : sort.trim().toUpperCase();

        if (s.isEmpty() || "LATEST".equals(s)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (s) {
            case "PRICE_DESC" ->
                    JpaSort.unsafe("settlement.totalPrice").descending();

            case "DIST_ASC" ->
                    Sort.by(Sort.Direction.ASC, "distance");

            case "START_ASC" ->
                    JpaSort.unsafe("snapshot.startSchedule").ascending();

            case "RECOMMEND" ->
                    Sort.by(Sort.Direction.ASC, "distance")
                            .and(JpaSort.unsafe("settlement.totalPrice").descending())
                            .and(JpaSort.unsafe("snapshot.startSchedule").ascending());

            default ->
                    Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private int safePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int safeSize(Integer size) {
        if (size == null) return 20;
        if (size < 1) return 20;
        if (size > 100) return 100;
        return size;
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}
