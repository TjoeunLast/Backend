package com.example.project.global.email;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

//EmailAuthRepository.java (Repository)
public interface EmailAuthRepository extends JpaRepository<EmailAuth, String> {
 void deleteByExpirationTimeBefore(LocalDateTime now);
}
