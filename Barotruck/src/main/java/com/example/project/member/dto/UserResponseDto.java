package com.example.project.member.dto;

import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String nickname;
    private String profileImageUrl;
    private String phone;
    private Role role;
    private Long ratingAvg;
    
 // 프론트의 DriverInfo, ShipperInfo 속성명에 대응
    @JsonProperty("DriverInfo")
    private DriverRequest driverInfo;

    @JsonProperty("ShipperInfo")
    private ShipperRequest shipperInfo;
    
    private String gender;   // 추가
    private Integer age;     // 추가
    // Neighborhood 정보 추가

    // 엔티티를 DTO로 변환하는 정적 메서드 (팩토리 메서드 패턴)
    public static UserResponseDto from(Users user) {
    	
    	
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname() != null ? user.getNickname() : "")
                .name(user.getName())
                .profileImageUrl(user.getProfileImage() != null ? user.getProfileImage().getImageUrl() : "")
                .phone(user.getPhone())
                .ratingAvg(user.getRatingAvg())
                .role(user.getRole())
                .driverInfo(DriverRequest.from(user.getDriver()))
                .shipperInfo(ShipperRequest.from(user.getShipper()))
                .gender(user.getGender()) // 추가
                .age(user.getAge())       // 추가
                .build();
    }
}
