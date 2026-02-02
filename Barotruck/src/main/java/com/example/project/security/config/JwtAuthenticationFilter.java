package com.example.project.security.config;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.project.security.token.TokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그용 (선택사항)

@Component
@RequiredArgsConstructor
@Slf4j // 로그를 찍고 싶다면 추가, 아니면 System.out.println 사용
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final TokenRepository tokenRepository;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    
    // 1. Auth 경로는 바로 통과
    if (request.getServletPath().contains("/api/v1/auth")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;
    
    // 2. 헤더가 없거나 Bearer 형식이 아니면 바로 통과 (비로그인)
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    
    // 🔥 [핵심 수정] 여기서부터 try-catch로 감쌉니다.
    try {
        jwt = authHeader.substring(7);
        
        // 3. 프론트엔드가 실수로 보낸 "null" 문자열 방어
        if (jwt == null || jwt.equals("null") || jwt.equals("undefined") || jwt.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 4. 토큰 해석 (여기서 만료되면 에러 발생 -> catch로 이동)
        userEmail = jwtService.extractUsername(jwt);
        
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
          
          var isTokenValid = tokenRepository.findByToken(jwt)
              .map(t -> !t.isExpired() && !t.isRevoked())
              .orElse(false);
          
          if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
            authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
          }
        }
    } catch (Exception e) {
        // 🔥 [예외 처리] 
        // 토큰이 만료되었거나(ExpiredJwtException) 형식이 잘못된 경우(MalformedJwtException)
        // 에러를 던지지 않고 로그만 남기고 넘어갑니다.
        // 결과적으로 SecurityContext가 비어있으므로 "익명 사용자"로 처리되어 permitAll 페이지에 접속 가능해집니다.
        System.out.println("⚠️ JWT 오류 발생 (비회원 처리): " + e.getMessage());
    }
    
    // 5. 다음 필터로 진행 (필수)
    filterChain.doFilter(request, response);
  }
}