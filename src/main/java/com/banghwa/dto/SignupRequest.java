package com.banghwa.dto;

import com.banghwa.model.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String username;
    private String name;
    private String birthDate;
    private String email;
    private String password;
    private String phoneNumber;
    private String gender;
    private UserRole role; // 관리자가 회원 등록 시 사용
    private Boolean deleted; // 삭제 여부 (관리자가 수정 시 사용)
}