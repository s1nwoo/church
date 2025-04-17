package com.banghwa.dto;

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
}