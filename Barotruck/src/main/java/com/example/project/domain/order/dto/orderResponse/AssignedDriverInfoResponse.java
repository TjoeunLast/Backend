package com.example.project.domain.order.dto.orderResponse;

import java.math.BigDecimal;

import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import lombok.*;

@Getter
@Builder
public class AssignedDriverInfoResponse {

    // 유저 정보 (차주 기본 정보)
    private Long userId;
    private String nickname;
    private String phone;

    // 드라이버 정보 (차량 정보)
    private Long driverId;
    private String carNum;
    private String carType;
    private BigDecimal tonnage;

    public static AssignedDriverInfoResponse from(Users user, Driver driver) {
        return AssignedDriverInfoResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .driverId(driver.getDriverId())
                .carNum(driver.getCarNum())
                .carType(driver.getCarType())
                .tonnage(driver.getTonnage())
                .build();
    }
}
