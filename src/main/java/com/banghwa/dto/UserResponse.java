package com.banghwa.dto;

import com.banghwa.model.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답 DTO
 * - JSON 직렬화 문제를 방지하기 위해 필요한 필드만 노출
 * - UserDetails의 authorities 등 민감한 정보 제외
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String birthDate;
    private String email;
    private String phoneNumber;
    private String role;  // UserRole enum을 String으로 변환
    private String gender;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Boolean deleted;
    private LocalDateTime deletedDate;

    /**
     * User 엔티티를 UserResponse로 변환
     * - authorities, password 등 민감 정보 제외
     */
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setBirthDate(user.getBirthDate());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole().name());  // UserRole → String (ADMIN, USER)
        response.setGender(user.getGender());
        response.setCreatedDate(user.getCreatedDate());
        response.setUpdatedDate(user.getUpdatedDate());
        response.setDeleted(user.getDeleted());
        response.setDeletedDate(user.getDeletedDate());
        return response;
    }
}