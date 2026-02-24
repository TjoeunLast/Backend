package com.example.project.global.fcm;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FCMConfig {

    @Value("${fcm.key.path:firebase-service-account.json}")
    private String fcmKeyPath;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream inputStream = new ClassPathResource(fcmKeyPath).getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                FirebaseApp.initializeApp(options);
                System.out.println("✅ FirebaseApp initialized successfully");
            }
        } catch (IOException e) {
            System.err.println("❌ Firebase init failed: " + e.getMessage());
        }
    }
}