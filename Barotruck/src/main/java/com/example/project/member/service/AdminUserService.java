package com.example.project.member.service;

import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.AdminUserResponse;
import com.example.project.member.dto.AdminUserResponse.AdminUserDetailResponse;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.token.TokenRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UsersRepository repository;
    private final OrderRepository orderRepository; // ✅ 주문/운행 레포지토리 주입
    private final TokenRepository tokenRepository;
    
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAdminUserList(Role role) {
        var sort = org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC,
            "enrolldate"
        );

        var users = (role == null)
            ? repository.findAll(sort)
            : repository.findAllByRole(role, sort);

        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse.AdminUserDetailResponse getAdminUserDetail(Long userId) {
    	// 1. 회원 조회 (중복 로직 제거 및 올바른 필드명 'repository' 사용)
        Users user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        
     // 2. 해당 유저의 누적 운행 건수를 DB에서 카운트
        // (OrderRepository에 해당 메서드가 정의되어 있어야 합니다.)
        Long totalCount = orderRepository.countByDriverNoAndStatus(userId, "COMPLETED");
        
        return AdminUserResponse.AdminUserDetailResponse.from(user, totalCount);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.suspendPermanently();
        revokeAllUserTokens(user);
        repository.save(user);
    }

    @Transactional
    public void suspendUserByDays(Long userId, int days) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        if (user.isPermanentlySuspended()) {
            throw new IllegalStateException("영구 정지 상태인 회원입니다.");
        }
        user.suspendTemporarily(LocalDateTime.now().plusDays(days));
        revokeAllUserTokens(user);
        repository.save(user);
    }

    @Transactional
    public void restoreUserById(Long userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.restore();
        repository.save(user);
    }

    @Transactional
    @Scheduled(cron = "${admin.user.suspension.release-cron:0 0 * * * *}")
    public void releaseExpiredSuspensions() {
        List<Users> expiredUsers = repository.findAllByDelflagAndSuspendedUntilLessThanEqual("A", LocalDateTime.now());
        expiredUsers.forEach(Users::restore);
    }

    private void revokeAllUserTokens(Users user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUserId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }
}
