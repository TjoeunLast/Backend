package com.example.project.global.hashid;

import org.hashids.Hashids;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class HashidsConfig {

    @Value("${hashids.salt}")
    private String salt;

    @Value("${hashids.min-length}")
    private int minLength;

    @Bean
    public Hashids hashids() {
        return new Hashids(salt, minLength);
    }
}