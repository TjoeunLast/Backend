package com.example.project.security.sms;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/sms")
@RequiredArgsConstructor
public class SmsAuthController {

    private final SmsAuthService smsAuthService;

    @PostMapping("/request")
    public ResponseEntity<Boolean> requestSms(@RequestParam("phone") String phone) {
        smsAuthService.sendAuthCode(phone);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifySms(
            @RequestParam("phone") String phone,
            @RequestParam("code") String code
    ) {
        boolean result = smsAuthService.verifyCode(phone, code);
        return ResponseEntity.ok(result);
    }
}
