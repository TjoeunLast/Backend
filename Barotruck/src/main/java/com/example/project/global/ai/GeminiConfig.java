package com.example.project.global.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties props) {
        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();
    }
}
