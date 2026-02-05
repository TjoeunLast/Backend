package com.example.project.domain.order.domain;

import java.time.LocalDateTime;

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
@Table(name = "ADMIN_CONTROLS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminControl {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Order order; // 이 필드에 대한 Setter가 필요함
	
    @Column(name = "IS_FORCED")
    private String isForced; // 강제변경여부 // "Y", "N"

    @Column(name = "PAID_ADMIN")
    private String paidAdmin; // 관리자 이메일

    @Column(name = "PAID_REASON")
    private String paidReason; // 강제변경사유
    
    @Column(name = "ALLOCATED")
    private LocalDateTime allocated; // 강제배차 일시
}
