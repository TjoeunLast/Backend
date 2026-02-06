package com.example.project.member.dto;

import java.math.BigDecimal;

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
}