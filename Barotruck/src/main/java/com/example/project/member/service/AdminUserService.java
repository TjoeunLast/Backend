package com.example.project.member.service;

import com.example.project.member.dto.AdminUserResponse;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UsersRepository repository;

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
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        return AdminUserResponse.AdminUserDetailResponse.from(user);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.setDelflag("A");
        user.setDeletedate(java.time.LocalDate.now());
        repository.save(user);
    }

    @Transactional
    public void restoreUserById(Long userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.setDelflag("N");
        user.setDeletedate(null);
        repository.save(user);
    }
}
