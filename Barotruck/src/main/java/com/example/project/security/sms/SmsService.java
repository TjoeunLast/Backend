package com.example.project.security.sms;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    private DefaultMessageService messageService;

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        // 서비스 초기화
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    public SingleMessageSentResponse sendSms(String toNumber, String text) {
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(toNumber);
        message.setText(text);

        return this.messageService.sendOne(new SingleMessageSendingRequest(message));
    }
}