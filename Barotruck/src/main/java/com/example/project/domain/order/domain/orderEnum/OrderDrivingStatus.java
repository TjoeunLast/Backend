package com.example.project.domain.order.domain.orderEnum;

import java.util.List;

public enum OrderDrivingStatus {
    REQUESTED, // 배차대기
    APPLIED, // 승인 대기
    ACCEPTED, // 배차확정
    LOADING, // 상차중
    IN_TRANSIT, // 이동중
    UNLOADING, // 하차중
    COMPLETED; // 하차완료

    public static List<String> asStrings() {
        return List.of(
                REQUESTED.name(),
                APPLIED.name(),
                ACCEPTED.name(),
                LOADING.name(),
                IN_TRANSIT.name(),
                UNLOADING.name(),
                COMPLETED.name()
        );
    }
}   
