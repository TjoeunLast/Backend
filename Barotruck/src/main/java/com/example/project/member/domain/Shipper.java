package com.example.project.member.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SHIPPER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHIPPER_ID")
    private Long shipperId;

    @Column(name = "COMPANY_NAME", length = 100)
    private String companyName;

    @Column(name = "BIZ_REG_NUM", length = 20)
    private String bizRegNum; // 사업자 등록 번호

    @Column(name = "REPRESENTATIVE", length = 50)
    private String representative; // 대표자명

    @Column(name = "BIZ_ADDRESS", length = 500)
    private String bizAddress; // 사업장 주소

    @Column(name = "IS_CORPORATE", length = 1)
    private String isCorporate; // 법인 여부 (Y/N)

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 공통 User 엔티티와의 연관 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = true)
    private Users user;
}
