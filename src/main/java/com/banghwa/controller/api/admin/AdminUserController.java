package com.banghwa.controller.api.admin;

import com.banghwa.dto.SignupRequest;
import com.banghwa.dto.UserResponse;
import com.banghwa.model.User;
import com.banghwa.model.UserRole;
import com.banghwa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 관리자 전용 - 성도(회원) 관리 API Controller
 *
 * 기능:
 * - 회원 목록 조회 (페이징, 검색)
 * - 회원 상세 조회
 * - 회원 등록
 * - 회원 수정
 * - 회원 삭제 (Soft Delete)
 */
@RestController
@RequestMapping("/api/admin/users")
@RolesAllowed("ADMIN") // 관리자만 접근 가능
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 회원 목록 조회 (페이징, 검색)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param keyword 검색 키워드 (이름/아이디/이메일)
     * @param includeDeleted 삭제된 회원 포함 여부
     * @return 회원 목록 + 페이징 정보 (UserResponse DTO로 변환)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<User> userPage;

            // 검색 조건에 따라 조회
            if (keyword != null && !keyword.trim().isEmpty()) {
                userPage = userRepository.searchUsers(keyword.trim(), includeDeleted, pageable);
            } else {
                if (includeDeleted) {
                    userPage = userRepository.findAll(pageable);
                } else {
                    userPage = userRepository.findByDeletedFalse(pageable);
                }
            }

            // User 엔티티를 UserResponse로 변환하여 응답
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent().stream()
                    .map(UserResponse::from)
                    .collect(Collectors.toList()));
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "회원 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 회원 상세 조회
     *
     * @param id 회원 ID
     * @return 회원 상세 정보 (UserResponse DTO로 변환)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "회원을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(UserResponse.from(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 회원 등록
     *
     * @param request 회원 등록 정보 (아이디, 비밀번호, 이름, 이메일, 권한 등)
     * @return 생성된 회원 정보 (UserResponse DTO로 변환)
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody SignupRequest request) {
        try {
            // 아이디 중복 체크
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "이미 사용 중인 아이디입니다."));
            }

            // 이메일 중복 체크
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "이미 사용 중인 이메일입니다."));
            }

            // 새 회원 생성 (Builder 패턴 사용)
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword())) // 비밀번호 암호화
                    .name(request.getName())
                    .birthDate(request.getBirthDate())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .gender(request.getGender())
                    .role(request.getRole() != null ? request.getRole() : UserRole.USER) // 기본값: USER
                    .createdDate(LocalDateTime.now())
                    .deleted(false)
                    .build();

            User savedUser = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 회원 수정
     *
     * @param id 회원 ID
     * @param request 수정할 회원 정보
     * @return 수정된 회원 정보 (UserResponse DTO로 변환)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody SignupRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "회원을 찾을 수 없습니다."));
            }

            User user = userOpt.get();

            // 아이디 변경 시 중복 체크
            if (!user.getUsername().equals(request.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "이미 사용 중인 아이디입니다."));
                }
                user.setUsername(request.getUsername());
            }

            // 이메일 변경 시 중복 체크
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
            user.setRole(request.getRole() != null ? request.getRole() : user.getRole());
            user.setUpdatedDate(LocalDateTime.now());

            // ✅ deleted 필드 업데이트 추가
            if (request.getDeleted() != null) {
                user.setDeleted(request.getDeleted());
                // 삭제 처리 시 deletedDate 설정
                if (request.getDeleted()) {
                    user.setDeletedDate(LocalDateTime.now());
                } else {
                    // 삭제 취소 시 deletedDate 제거
                    user.setDeletedDate(null);
                }
            }

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(UserResponse.from(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 수정 실패: " + e.getMessage()));
        }
    }

    /**
     * 회원 삭제 (Soft Delete)
     *
     * @param id 회원 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "회원을 찾을 수 없습니다."));
            }

            User user = userOpt.get();
            user.setDeleted(true);
            user.setDeletedDate(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "회원이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 삭제 실패: " + e.getMessage()));
        }
    }
}