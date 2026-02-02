package com.example.project.security.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleAuthenticationResponse {
    private String token;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private boolean newUser; // Indicates if the user was newly registered (renamed from isNewUser)

    @JsonProperty("onboarding_survey_completed")
    private Boolean onboardingSurveyCompleted; // 온보딩 설문 완료 여부

    private String email; // Added for new user registration flow
    private String nickname; // Added for new user registration flow
    private String error; // Optional: for conveying error messages
}
