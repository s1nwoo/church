// com.banghwa.dto.SignupRequest.java
package com.banghwa.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {
    private String name;
    private LocalDate birthDate;
    private String email;
    private String password;
    private String phoneNumber;
}
