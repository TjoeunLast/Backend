package com.example.project.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.project.global.hashid.HashidToLongConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**") 
        .allowedOrigins(
	                "http://localhost:5173",   // Vite
	                "http://localhost:3000",   // React
	                "http://localhost:8080",   // Flutter Web (기본값인 경우가 많음)
	                "http://localhost:50123",   // 실제 플러터 실행 시 터미널에 뜨는 포트 번호 확인 필요
	                "http://localhost:49622"  // <--- 현재 플러터 웹 주소 추가!
        		)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true);
    }
    
    private final HashidToLongConverter hashidToLongConverter;

    public WebConfig(HashidToLongConverter hashidToLongConverter) {
        this.hashidToLongConverter = hashidToLongConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(hashidToLongConverter);
    }
}
