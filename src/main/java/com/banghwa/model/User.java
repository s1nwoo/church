package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private String birthDate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedDate;

    @Column(nullable = false)
    private String gender; // "남자" or "여자"

    // ✅ [Security] 사용자 권한 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // ✅ [Security] 사용자 이름 = username
    @Override
    public String getUsername() {
        return username;
    }

    // ✅ [Security] 사용자 비밀번호
    @Override
    public String getPassword() {
        return password;
    }

    // ✅ [Security] 계정 만료 여부
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // ✅ [Security] 계정 잠김 여부
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // ✅ [Security] 자격 증명 만료 여부
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ✅ [Security] 계정 활성화 여부
    @Override
    public boolean isEnabled() {
        return !deleted;
    }
}
