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
                .allowedOriginPatterns("*") // 모든 도메인 허용 (핵심!)
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
