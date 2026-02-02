package com.example.project.security.sms;


import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsAuthService {

    private final SmsAuthRepository smsAuthRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private DefaultMessageService messageService;

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    // 1. 인증 코드 발송 및 DB 저장
    @Async
    @Transactional
    public void sendAuthCode(String phone) {
        String authCode = String.format("%06d", secureRandom.nextInt(1000000));
        
        SmsAuth smsAuth = smsAuthRepository.findById(phone)
                .orElse(new SmsAuth());
        
        smsAuth.setPhone(phone);
        smsAuth.setAuthCode(authCode);
        smsAuth.setExpirationTime(LocalDateTime.now().plusMinutes(3)); // SMS는 보통 3분 유효
        
        smsAuthRepository.save(smsAuth);
        sendRealSms(phone, authCode);
    }

    // 2. 실제 솔라피 SMS 발송
    protected void sendRealSms(String phone, String authCode) {
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(phone);
        message.setText("[SmartRoutePlanner] 인증번호: " + authCode + "\n3분 내에 입력해주세요.");

        try {
            this.messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("인증 문자 발송 성공: {}", phone);
        } catch (Exception e) {
            log.error("문자 발송 실패: phone={}", phone, e);
        }
    }

    // 3. 코드 검증
    @Transactional
    public boolean verifyCode(String phone, String inputCode) {
        return smsAuthRepository.findById(phone)
                .filter(auth -> !auth.isExpired() && auth.getAuthCode().equals(inputCode))
                .map(auth -> {
                    smsAuthRepository.delete(auth); // 인증 성공 시 삭제
                    return true;
                })
                .orElse(false);
    }

    // 4. 클린업 스케줄러 (매시간 실행)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredAuth() {
        smsAuthRepository.deleteByExpirationTimeBefore(LocalDateTime.now());
        log.info("만료된 SMS 인증 데이터 삭제 완료");
    }
}