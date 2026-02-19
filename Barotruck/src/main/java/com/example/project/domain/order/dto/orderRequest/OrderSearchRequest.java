package com.example.project.domain.order.dto.orderRequest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderSearchRequest {

    // 탭
    private String tab; // ALL, RECOMMEND

    // 지역
    private String puProvince; // 상차 시/도
    private String doProvince; // 하차 시/도

    // 차량
    private String reqCarType;     // 윙바디, 카고 등
    private String reqTonnage;  // 최대 톤수

    // 업무 / 운행
    private String workType;   // 독차, 혼적
    private String driveMode;  // 바로배차 등

    // 시간
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTo;

    // 정렬
    private String sort; // RECOMMEND, LATEST, PRICE_DESC, DIST_ASC

    // 페이징
    private Integer page = 0;
    private Integer size = 20;
}
