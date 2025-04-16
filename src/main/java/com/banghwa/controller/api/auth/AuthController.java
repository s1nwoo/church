package com.banghwa.controller.api.auth;

import com.banghwa.dto.SignupRequest;
import com.banghwa.dto.LoginRequest;
import com.banghwa.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpSession session) {
        authService.login(request, session);
        return ResponseEntity.ok("로그인 성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate(); // 세션 무효화
        return ResponseEntity.ok("로그아웃 완료");
    }
}
