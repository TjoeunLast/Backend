package com.example.project.security.sms;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SmsService smsService;

    @PostMapping("/api/v1/auth/sms")
    public ResponseEntity<String> sendVerificationCode(@RequestParam("phoneNumber") String phoneNumber) {
    	String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));        
    	smsService.sendSms(phoneNumber, "[운송플랫폼] 인증번호는 [" + code + "] 입니다.");
    	
    	
        return ResponseEntity.ok("메시지 발송 성공");
    }
}
