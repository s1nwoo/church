package com.banghwa.controller.api.auth;

import com.banghwa.dto.LoginRequest;
import com.banghwa.dto.UserInfoResponse;
import com.banghwa.model.User;
import com.banghwa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 로그인 시 사용자 정보 반환
    @PostMapping("/login")
    public ResponseEntity<UserInfoResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole().name());

        // ✅ 사용자 정보를 응답으로 전달
        return ResponseEntity.ok(new UserInfoResponse(
                user.getName(),
                user.getUsername(),
                user.getEmail()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        return ResponseEntity.ok(new UserInfoResponse(
                user.getName(),
                user.getUsername(),
                user.getEmail()
        ));
    }
}
