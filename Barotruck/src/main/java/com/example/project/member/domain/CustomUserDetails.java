package com.example.project.member.domain;


import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final Role role;
    private final boolean isEnabled;
    
    
    public CustomUserDetails(Users user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.role = user.getRole();
        this.isEnabled = "N".equals(user.getDelflag());
        // 비밀번호는 아예 필드로도 받지 않습니다.
    }

    @Override
    public String getPassword() {
        return null; // 인증이 완료된 후에는 비밀번호 정보가 필요 없습니다.
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // 계정 상태 체크 (기본 보안 설정)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return this.isEnabled; }
}


