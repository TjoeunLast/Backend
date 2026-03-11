package com.example.project.domain.payment.port;

public interface UserPort {

    UserInfo getRequiredUser(Long userId);

    UserInfo lockRequiredUser(Long userId);

    record UserInfo(
            Long userId,
            Long userLevel
    ) {}
}
