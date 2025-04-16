package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users") // 테이블명 users
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Column(unique = true)
    private String email;

    private String password;

    private String phoneNumber;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private Boolean deleted = false;
    private LocalDateTime deletedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = this.createdDate;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
