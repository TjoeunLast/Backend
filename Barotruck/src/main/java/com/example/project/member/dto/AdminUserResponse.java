package com.example.project.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal; // ✅ 이 줄을 추가하세요
import com.example.project.global.image.ImageInfo;
import com.example.project.member.domain.Users;

import lombok.*;

@Getter
@AllArgsConstructor
public class AdminUserResponse {
	private Long userId; // ✅ userId 추가
    private String role;
    private String nickname;
    private String phone;
    private String email;
    private LocalDate enrolldate;
    private String delflag;

    public static AdminUserResponse from(Users user) {
        return new AdminUserResponse(
        		user.getUserId(), // ✅ 생성자에 userId 매핑 추가
                user.getRole() != null ? user.getRole().name() : null,
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getEnrolldate(),
                user.getDelflag()
        );
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDetailResponse {
    	// Users 필드
        private Long userId;
        private Integer age;
        private LocalDate deletedate;
        private String delflag;
        private String email;
        private LocalDate enrolldate;
        private String gender;
        private String nickname;
        private String password;
        private String phone;
        private String imageUrl;
        private String originalName;
        private Long ratingAvg;
        private String regflag;
        private String isOwner;
        private LocalDateTime rate;
        private Long userLevel;
        // ✅ 1. 누적 운행 건수 필드 추가
        private Long totalOperationCount;
        
        // DRIVERS 필드
        private String carNum; // CAR_NUM
        private String carType;     // CAR_TYPE
        private BigDecimal tonnage; // TONNAGE
        private String type;        // TYPE (냉장 등)
        private String bankName;    // BANK_NAME
        private String accountNum;  // ACCOUNT_NUM
        
        // SHIPPER 필드
        private String companyName;  // COMPANY_NAME
        private String bizRegNum;    // BIZ_REG_NUM
        private String representative;// REPRESENTATIVE
        private String bizAddress;   // BIZ_ADDRESS
        
        public static AdminUserDetailResponse from(com.example.project.member.domain.Users user, Long totalCount) {
            ImageInfo imageInfo = user.getProfileImage();

            AdminUserDetailResponseBuilder builder = AdminUserDetailResponse.builder()
                    .userId(user.getUserId())
                    .age(user.getAge())
                    .deletedate(user.getDeletedate())
                    .delflag(user.getDelflag())
                    .email(user.getEmail())
                    .enrolldate(user.getEnrolldate())
                    .gender(user.getGender())
                    .nickname(user.getNickname())
                    // null, // password는 내려주지 말기
                    .phone(user.getPhone())
                    .imageUrl(imageInfo != null ? imageInfo.getImageUrl() : null)
                    .originalName(imageInfo != null ? imageInfo.getOriginalName() : null)
                    .ratingAvg(user.getRatingAvg())
                    .regflag(user.getRegflag())
                    .isOwner(user.getRole() != null ? user.getRole().name() : null)
                    .rate(user.getRate())
                    .userLevel(user.getUser_level())
            		.totalOperationCount(totalCount); // ✅ 3. 값 매핑
            
            // 차주 정보 매핑
            if ("DRIVER".equals(builder.isOwner) && user.getDriver() != null) {
                builder.carNum(user.getDriver().getCarNum())
                       .carType(user.getDriver().getCarType())
                       .tonnage(user.getDriver().getTonnage())
                       .type(user.getDriver().getType())
                       .bankName(user.getDriver().getBankName())
                       .accountNum(user.getDriver().getAccountNum());
            }
            
            // 화주 정보 매핑 (SHIPPER 테이블 데이터)
            if ("SHIPPER".equals(builder.isOwner) && user.getShipper() != null) {
                builder.companyName(user.getShipper().getCompanyName())
                       .bizRegNum(user.getShipper().getBizRegNum())
                       .representative(user.getShipper().getRepresentative())
                       .bizAddress(user.getShipper().getBizAddress());
            }

            return builder.build();
        }

    }
}
