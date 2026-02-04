package com.example.project.security.config;


import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

	// 인증 없이 접근 가능한 URL 목록
    private static final String[] WHITE_LIST_URL = {
            "/api/v1/auth/**",
            "/api/auth/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html",
            "/index.html", 
            "/login.html", 
            "/register.html",
            "/css/**", 
            "/js/**",
            "/api/recipes/**",
            "/api/receipt/**",
            "/api/v1/shippers",
            "/api/v1/drivers",
            "/api/v1/orders", // 오더
            "/api/notifications/**",  // 알림
            "/api/reports/**", // 신고 
            "/api/reviews/**", // 리뷰
            "/ws-stomp/**",      // 1. 웹소켓 엔드포인트 추가
            "/pub/**",           // 2. 메시지 발행 경로 추가 (컨트롤러 입구)
            "/sub/**" ,			
            "/api/chat/room/**",
            "/api/v1/auth/sms",
            "/api/auth/sms",	// sms
            
            
    };
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST API이므로 CSRF 비활성화
                .authorizeHttpRequests(req ->
                        req.requestMatchers(WHITE_LIST_URL).permitAll() // 화이트리스트 허용
                                .requestMatchers(GET, "/api/user/check-nickname").permitAll() // 중복체크 허용
                                .requestMatchers(GET, "/api/neighborhoods/**").permitAll() // 지역검색 허용
                                .requestMatchers("/ws-stomp/**").permitAll() // 웹소켓 핸드쉐이크 허용
                                .requestMatchers(GET, "/api/route/**").permitAll()
                                .requestMatchers("/api/ocr/**").permitAll()
                                .requestMatchers("/api/**").permitAll()
                                
                                // ★ 중요: 채팅과 공구 관련 API는 반드시 인증(Token) 필요
                                // 이렇게 설정해야 @AuthenticationPrincipal에 데이터가 정상적으로 들어옵니다.
                                .requestMatchers("/api/chat/**").authenticated()
                                .requestMatchers("/api/user/**").authenticated() // 유저 정보 관련 추가
                                
                                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS)) // 세션 미사용
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout.logoutUrl("/api/v1/auth/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
                )
        ;

        return http.build();
    }
}
