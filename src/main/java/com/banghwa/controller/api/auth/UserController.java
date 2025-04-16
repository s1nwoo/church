package com.banghwa.controller.api.auth;

import com.banghwa.dto.SignupRequest;
import com.banghwa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }
}
