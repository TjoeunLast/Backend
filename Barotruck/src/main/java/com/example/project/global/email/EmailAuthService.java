package com.example.project.global.email;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final JavaMailSender mailSender;
    private final EmailAuthRepository emailAuthRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // 1. 인증 코드 발송 및 DB 저장
    @Async
    @Transactional
    public void sendAuthCode(String toEmail) {
        String authCode = String.format("%06d", secureRandom.nextInt(1000000));
        
        EmailAuth emailAuth = emailAuthRepository.findById(toEmail)
                .orElse(new EmailAuth());
        
        emailAuth.setEmail(toEmail);
        emailAuth.setAuthCode(authCode);
        emailAuth.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        
        emailAuthRepository.save(emailAuth);
        sendEmail(toEmail, authCode);
    }

    // 2. 실제 메일 발송
    protected void sendEmail(String toEmail, String authCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[SmartRoutePlanner] 인증번호");
        message.setText("인증번호: " + authCode + "\n5분 내에 입력해주세요.");
        
        try {
            mailSender.send(message);
            log.info("인증 메일 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("메일 발송 실패: toEmail={}", toEmail, e);
        }
    }

    // 3. 코드 검증
    @Transactional
    public boolean verifyCode(String email, String inputCode) {
        return emailAuthRepository.findById(email)
                .filter(auth -> !auth.isExpired() && auth.getAuthCode().equals(inputCode))
                .map(auth -> {
                    emailAuthRepository.delete(auth); // 인증 성공 시 즉시 삭제
                    return true;
                })
                .orElse(false);
    }

    // 4. SQL 클린업: 매시간 만료된 데이터 삭제 (Redis의 TTL 기능 대체)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredAuth() {
        emailAuthRepository.deleteByExpirationTimeBefore(LocalDateTime.now());
        log.info("만료된 이메일 인증 데이터 삭제 완료");
    }
}