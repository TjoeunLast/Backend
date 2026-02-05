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
@Table(name = "DRIVERS")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DRIVER_ID")
    private Long driverId; // 

    @Column(name = "CAR_NUM", length = 20)
    private String carNum; // 차 번호

    @Column(name = "CAR_TYPE", length = 50)
    private String carType; // 차 종류

    @Column(name = "TONNAGE")
    private Long tonnage;

    @Column(name = "CAREER")
    private Long career; // 경력

    @Column(name = "BANK_NAME", length = 50)
    private String bankName; // 은행이름

    @Column(name = "ACCOUNT_NUM", length = 50)
    private String accountNum; // 은행번호

    @Column(name = "IS_SUSPENDED", length = 1)
    private String isSuspended; // 정지 여부 (Y/N)

    @Column(name = "TYPE")
    private String type; // 냉장, 냉동, 기타 등등

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 공통 User 엔티티와의 연관 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = true)
    private Users user;


}
