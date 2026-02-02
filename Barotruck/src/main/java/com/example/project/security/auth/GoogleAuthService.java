package com.example.project.security.auth;

import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.config.JwtService;
import com.example.project.security.token.Token;
import com.example.project.security.token.TokenRepository;
import com.example.project.security.token.TokenType;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // 이 부분을 추가하세요
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    @Value("${google.oauth.client-id}")
    private String googleClientId;


    private final UsersRepository usersRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager; // Spring Security AuthenticationManager
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthenticationResponse authenticateGoogleUser(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        log.info("Attempting to verify Google ID token...");
        GoogleIdToken googleIdToken = verifier.verify(idToken);
        log.info("Google ID token verified successfully.");

        if (googleIdToken != null) {
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            Optional<Users> existingUser = usersRepository.findByEmail(email);

            if (existingUser.isEmpty()) {
                // User is new: DO NOT save user yet, just return newUser flag.
                // Frontend will then navigate to GoogleSignUpScreen to collect more info.
                return GoogleAuthenticationResponse.builder()
                        .token(null) // No JWT token yet for new users
                        .newUser(true) // Indicate it's a new user
                        .email(email) // Pass email for signup screen
                        .nickname(name) // Pass nickname for signup screen
                        .onboardingSurveyCompleted(false) // For new users, this is always false.
                        .build();
            } else {
                // User exists: Authenticate and generate JWT token.
                Users user = existingUser.get();

                // Authenticate the user in Spring Security context
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                "google_oauth_user_no_password" // Use the raw placeholder password for authentication context
                        )
                );

                var jwtToken = jwtService.generateToken(user);
                var refreshToken = jwtService.generateRefreshToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, jwtToken);

                return GoogleAuthenticationResponse.builder()
                        .token(jwtToken)
                        .refreshToken(refreshToken)
                        .newUser(false) // Indicate it's an existing user
                        .email(email) // Include email for consistency
                        .nickname(name) // Include nickname for consistency
                        .build();
            }

        } else {
            throw new IllegalArgumentException("Invalid Google ID Token");
        }
    }

    private void saveUserToken(Users user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(Users user) {
        var validUserTokens =
                tokenRepository.findAllValidTokenByUser(user.getUserId());

        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }
}