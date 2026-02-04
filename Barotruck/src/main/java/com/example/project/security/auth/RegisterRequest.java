package com.example.project.security.auth;

import com.example.project.security.user.Role;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

	// 기존 firstname, lastname 삭제 -> nickname으로 통합
	  private String nickname;
	  private String email;
	  private String password;
      private String phone;
      @Enumerated(EnumType.STRING) // DB에 숫자가 아닌 "SHIPPER" 문자열로 저장됨
      @NotNull
      private Role role; // "SHIPPER" 또는 "DRIVER"
	  
	  // 새로 추가된 필드들 (DB 스키마 반영)
	  private String gender; // M or F
	  private Integer age;
	  
	  // dto
	  private ShipperDto shipper;
	  private DriverDto driver;
}
@Data
class ShipperDto {
    private String companyName;
    private String bizRegNum;
    private String representative;
    private String bizAddress;
}

@Data
class DriverDto {
    private String carNum;
    private String carType;
    private Long tonnage;
    private String bankName;
    private String accountNum;
}