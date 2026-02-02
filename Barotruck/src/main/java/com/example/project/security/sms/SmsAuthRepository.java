package com.example.project.security.sms;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsAuthRepository extends JpaRepository<SmsAuth, String> {
    void deleteByExpirationTimeBefore(LocalDateTime now);
}
