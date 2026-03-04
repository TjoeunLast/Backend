package com.example.project.member.dto;

import java.math.BigDecimal;

import com.example.project.member.domain.Driver;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequest {
    @NotBlank(message = "차량 번호는 필수입니다.")
    private String carNum;

    @NotBlank(message = "차량 종류는 필수입니다.")
    private String carType;

    private BigDecimal tonnage;
    private Long career;
    private String bankName;
    private String accountNum;
    private String type; // 냉장, 냉동 등
    
    private Long nbhId;
    private String address;
    
    public static DriverRequest from(Driver driver) {
        if (driver == null) return null;

        return DriverRequest.builder()
                .carNum(driver.getCarNum())
                .carType(driver.getCarType())
                .tonnage(driver.getTonnage())
                .career(driver.getCareer())
                .bankName(driver.getBankName())
                .accountNum(driver.getAccountNum())
                .type(driver.getType())
                .nbhId(driver.getNbhId())   // 동네/지역 ID
                .address(driver.getAddress()) // 주소
                .build();
    }
}