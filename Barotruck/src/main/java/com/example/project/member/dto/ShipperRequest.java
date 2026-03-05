package com.example.project.member.dto;

import com.example.project.member.domain.Shipper;

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
    
    
    public static ShipperRequest from(Shipper shipper) {
    	if (shipper == null) return null; // 중요: 정보가 없으면 null 반환
    	
        return ShipperRequest.builder()
                .companyName(shipper.getCompanyName())   // 차주 엔티티의 회사명
                .bizRegNum(shipper.getBizRegNum())       // 차주 엔티티의 사업자 번호
                .representative(shipper.getRepresentative()) // 차주 엔티티의 대표자명
                .bizAddress(shipper.getBizAddress())     // 차주 엔티티의 사업장 주소
                .isCorporate(shipper.getIsCorporate() != null ? shipper.getIsCorporate() : "N") // 법인 여부 (기본값 N)
                .build();
    }
}