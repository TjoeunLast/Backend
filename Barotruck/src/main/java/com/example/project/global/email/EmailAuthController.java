package com.example.project.global.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/email-request")
    public ResponseEntity<Boolean> requestEmail(@RequestParam("email") String email) { // ("email") 추가
        emailAuthService.sendAuthCode(email);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/email-verify")
    public ResponseEntity<Boolean> verifyEmail(
            @RequestParam("email") String email, // ("email") 추가
            @RequestParam("code") String code    // ("code") 추가
    ) {
        boolean result = emailAuthService.verifyCode(email, code);
        return ResponseEntity.ok(result);
    }
}
