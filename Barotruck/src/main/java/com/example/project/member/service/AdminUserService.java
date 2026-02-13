package com.example.project.member.service;

import com.example.project.member.dto.AdminUserListResponse;
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
        public List<AdminUserListResponse> getAdminUserList(Role role) {
        var sort = org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC,
            "enrolldate"
        );

        var users = (role == null)
            ? repository.findAll(sort)
            : repository.findAllByRole(role, sort);

        return users.stream()
            .map(AdminUserListResponse::from)
            .toList();
    }
}
