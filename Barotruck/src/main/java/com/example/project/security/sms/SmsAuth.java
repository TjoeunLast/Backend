package com.example.project.security.sms;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sms_auth", indexes = @Index(name = "idx_sms_auth_expiration", columnList = "expirationTime"))
@Getter @Setter
@NoArgsConstructor
public class SmsAuth {
    @Id
    private String phone; // 휴대폰 번호를 식별자로 사용

    @Column(nullable = false, length = 6)
    private String authCode;

    @Column(nullable = false)
    private LocalDateTime expirationTime;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationTime);
    }
}