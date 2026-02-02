package com.example.project.global.email;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_auth", indexes = @Index(name = "idx_email_auth_expiration", columnList = "expirationTime"))
@Getter @Setter
@NoArgsConstructor
public class EmailAuth {
    @Id
    private String email;

    @Column(nullable = false, length = 6)
    private String authCode;

    @Column(nullable = false)
    private LocalDateTime expirationTime;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationTime);
    }
}