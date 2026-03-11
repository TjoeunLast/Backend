package com.example.project.domain.payment.port.impl;

/**
 * JpaUserPort
 *
 * 역할:
 * - UserPort 인터페이스를 JPA 방식으로 구현한 클래스
 * - 결제 도메인이 필요한 유저 정보를 DB에서 조회
 *
 * 구조:
 * PaymentService → UserPort → JpaUserPort → UsersRepository → DB
 *
 * 결제 서비스는 UsersRepository를 직접 모른다.
 * 이 클래스가 대신 조회해준다.
 */

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.payment.port.UserPort;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaUserPort implements UserPort {

    /**
     * 실제 DB 접근을 담당하는 JPA Repository
     */
    private final UsersRepository usersRepository;


    /**
     * 결제에 반드시 필요한 유저 정보를 조회한다.
     *
     * 동작:
     * 1. userId로 Users 엔티티 조회
     * 2. 없으면 예외 발생
     * 3. user_level이 null이면 기본값 1로 설정
     * 4. 결제 도메인 전용 DTO(UserInfo)로 변환하여 반환
     *
     * 특징:
     * - Users 엔티티 전체를 반환하지 않는다.
     * - 결제에 필요한 최소 정보만 전달한다.
     */
    @Override
    @Transactional(readOnly = true)
    public UserInfo getRequiredUser(Long userId) {
        return toUserInfo(
                usersRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId))
        );
    }

    @Override
    @Transactional
    public UserInfo lockRequiredUser(Long userId) {
        return toUserInfo(
                usersRepository.findByIdForUpdate(userId)
                        .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId))
        );
    }

    private UserInfo toUserInfo(Users user) {
        Long level = user.getUser_level();
        if (level == null) {
            level = 0L;
        }
        return new UserInfo(user.getUserId(), level);
    }
}
