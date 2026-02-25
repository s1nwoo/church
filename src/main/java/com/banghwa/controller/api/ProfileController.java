package com.banghwa.controller.api;

import com.banghwa.dto.SignupRequest;
import com.banghwa.dto.UserResponse;
import com.banghwa.model.User;
import com.banghwa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 내 정보 관리 API Controller
 *
 * 기능:
 * - 로그인한 사용자의 정보 조회
 * - 로그인한 사용자의 정보 수정
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 현재 로그인한 사용자 정보 조회
     *
     * @return 사용자 정보 (UserResponse DTO)
     */
    @GetMapping
    public ResponseEntity<?> getMyProfile() {
        try {
            // SecurityContext에서 현재 로그인한 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(UserResponse.from(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "정보 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 현재 로그인한 사용자 정보 수정
     *
     * @param request 수정할 정보
     * @return 수정된 사용자 정보 (UserResponse DTO)
     */
    @PutMapping
    public ResponseEntity<?> updateMyProfile(@RequestBody SignupRequest request) {
        try {
            // SecurityContext에서 현재 로그인한 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }

            User user = userOpt.get();

            // 아이디 변경 시 중복 체크 (자신의 아이디가 아닌 경우만)
            if (!user.getUsername().equals(request.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "이미 사용 중인 아이디입니다."));
                }
                user.setUsername(request.getUsername());
            }

            // 이메일 변경 시 중복 체크 (자신의 이메일이 아닌 경우만)
            if (!user.getEmail().equals(request.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "이미 사용 중인 이메일입니다."));
                }
                user.setEmail(request.getEmail());
            }

            // 비밀번호가 제공된 경우에만 변경
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            // 나머지 필드 업데이트
            user.setName(request.getName());
            user.setBirthDate(request.getBirthDate());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setGender(request.getGender());
            user.setUpdatedDate(LocalDateTime.now());

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(UserResponse.from(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "정보 수정 실패: " + e.getMessage()));
        }
    }
}