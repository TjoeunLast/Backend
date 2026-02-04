package com.example.project.member.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.project.global.image.ImageInfo;
import com.example.project.global.image.ImageUploadResponse;
import com.example.project.security.token.Token;
import com.example.project.security.user.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "USERS")
public class Users implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user_id_gen")
    @SequenceGenerator(
            name = "seq_user_id_gen",
            sequenceName = "SEQ_USER_ID",
            allocationSize = 1
    )
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String nickname;

    @Column(length = 1)
    private String gender;
    
    private Integer age;

    @Column(length = 100, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_owner", length = 20)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private LocalDate enrolldate = LocalDate.now();

    @Builder.Default
    @Column(length = 1)
    private String delflag = "N";

    private LocalDate deletedate;
    @Column(length = 1)
    private String regflag;

    @Column(length = 300)
    private String password;

    @OneToMany(mappedBy = "user")
    private List<Token> tokens;

    private String phone;

    private Long ratingAvg;

    
 // User.java 내부
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Shipper shipper;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Driver driver;
    
    

 // 기존 코드에 필드 추가
    @Column(name = "fcm_token")
    private String fcmToken; // Flutter 앱에서 발급받아 서버로 보내준 토큰 저장

    // 토큰 업데이트 메서드
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    @Embedded
    private ImageInfo profileImage;

 // 이미지 업데이트 편의 메서드
    public void updateProfileImage(ImageUploadResponse res) {
        this.profileImage = new ImageInfo(res);
    }

    // 이미지 삭제(초기화) 메서드
    public void clearProfileImage() {
        this.profileImage = null;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() { 
        return email; 
    }

    @Override
    public String getPassword() { 
        return password; 
    }

    @Override
    public boolean isAccountNonExpired() { 
        return true; 
    }

    @Override
    public boolean isAccountNonLocked() { 
        return true; 
    }

    @Override
    public boolean isCredentialsNonExpired() { 
        return true; 
    }

    @Override
    public boolean isEnabled() { 
        return "N".equals(delflag); 
    }
}
