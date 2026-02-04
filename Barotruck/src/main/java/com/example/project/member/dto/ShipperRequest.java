package com.example.project.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperRequest {
    @NotBlank(message = "회사명은 필수입니다.")
    private String companyName;

    @NotBlank(message = "사업자 번호는 필수입니다.")
    @Pattern(regexp = "\\d{3}-\\d{2}-\\d{5}", message = "사업자 번호 형식이 올바르지 않습니다. (예: 123-45-67890)")
    private String bizRegNum;

    @NotBlank(message = "대표자명은 필수입니다.")
    private String representative;

    private String bizAddress;
    
    @Builder.Default
    private String isCorporate = "N"; // Y or N
}