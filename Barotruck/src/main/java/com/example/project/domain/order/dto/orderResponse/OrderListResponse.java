package com.example.project.domain.order.dto.orderResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderListResponse {

    private Long orderId;

    private String startPlace;
    private String endPlace;

    private Long distance;
    private Long totalPrice;

    // 바꿈: LocalDateTime -> String
    private String startSchedule;

    private String reqCarType;

    // 바꿈: Double -> String
    private String reqTonnage;

    private String workType;

    private Boolean aiRecommended;
    private String driveMode;
    private String status;
}
