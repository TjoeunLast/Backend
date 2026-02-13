package com.example.project.domain.order.repository;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.orderRequest.OrderSearchRequest;
import org.springframework.data.jpa.domain.Specification;

public class OrderSearchSpecifications {

    public static Specification<Order> byFilter(OrderSearchRequest f) { // 동적쿼리 생성 클래스
        return (root, query, cb) -> {
            var p = cb.conjunction();

            // snapshot 경로
            var s = root.get("snapshot");

            // 지역
            if (notBlank(f.getPuProvince())) {
                p = cb.and(p, cb.equal(s.get("puProvince"), f.getPuProvince()));
            }
            if (notBlank(f.getDoProvince())) {
                p = cb.and(p, cb.equal(s.get("doProvince"), f.getDoProvince()));
            }

            // 차량
            if (notBlank(f.getReqCarType())) {
                p = cb.and(p, cb.equal(s.get("reqCarType"), f.getReqCarType()));
            }
            if (notBlank(f.getReqTonnage())) {
                p = cb.and(p, cb.equal(s.get("reqTonnage"), f.getReqTonnage()));
            }

            // 업무/운행
            if (notBlank(f.getWorkType())) {
                p = cb.and(p, cb.equal(s.get("workType"), f.getWorkType()));
            }
            if (notBlank(f.getDriveMode())) {
                p = cb.and(p, cb.equal(s.get("driveMode"), f.getDriveMode()));
            }

            // 시간(출발)
            if (f.getStartFrom() != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(s.get("startSchedule"), f.getStartFrom()));
            }
            if (f.getStartTo() != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(s.get("startSchedule"), f.getStartTo()));
            }

            // 탭(예: RECOMMEND/ALL) - 지금은 값만 들고 있고 필터링은 서비스에서 ai 플래그만 주는 상태
            // 필요하면 여기서 status 조건 추가하면 됨.

            return p;
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
