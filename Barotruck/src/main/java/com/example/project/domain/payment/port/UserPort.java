package com.example.project.domain.payment.port;

public interface UserPort {

    UserInfo getRequiredUser(Long userId);

    record UserInfo(
            Long userId,
            Long userLevel
    ) {}
}
