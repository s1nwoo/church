package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String writer;

    private String category;

    @Lob
    @Column(nullable = true)
    private String content;

    private LocalDateTime createdDate;   // ✅ 생성일
    private LocalDateTime updatedDate;   // ✅ 수정일

    private Boolean deleted = false;     // ✅ 삭제 여부
    private LocalDateTime deletedDate;   // ✅ 삭제일

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
