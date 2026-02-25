package com.banghwa.controller.api.auth;

import com.banghwa.dto.LoginRequest;
import com.banghwa.model.User;
import com.banghwa.repository.UserRepository;
import com.banghwa.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {

        // 1) 스프링 시큐리티로 인증
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2) JWT 토큰 생성
        String token = jwtUtil.generateToken(auth.getName());

        // 3) 시큐리티 UserDetails
        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(autho -> autho.getAuthority()) // 예: "ROLE_ADMIN"
                .orElse("ROLE_USER");

        // 4) 우리 도메인 User 엔티티 조회 (이름, 이메일 위해)
        User domainUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        // 5) 프론트로 내려줄 정보
        return Map.of(
                "id", domainUser.getId(),
                "username", domainUser.getUsername(),
                "name", domainUser.getName(),
                "email", domainUser.getEmail(),
                "role", role,
                "accessToken", token
        );
    }
}
