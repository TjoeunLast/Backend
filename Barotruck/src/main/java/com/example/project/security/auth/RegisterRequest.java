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

    private String name;      // 추가
    private String nickname;
    private String email;
    private String password;
    private String phone;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Role role;

    private String gender;
    private Integer age;

    private ShipperDto shipper;
    private DriverDto driver;
}

@Data
class ShipperDto {
    private String companyName;
    private String bizRegNum;
    private String representative;
    private String bizAddress;
    private String isCorporate; // Y: 사업자, N: 개인화주
}

@Data
class DriverDto {
    private String carNum;
    private String carType;
    private Long tonnage;
    private String bankName;
    private String accountNum;
    private String address;
}
