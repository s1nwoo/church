package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sermon_video")
public class SermonVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK: 설교 영상 고유 ID

    @Column(nullable = false, length = 255)
    private String title; // 영상 제목

    @Column(name = "youtube_url", nullable = false, length = 255)
    private String youtubeUrl; // YouTube 영상 URL 또는 ID

    @Column(nullable = false, length = 100)
    private String preacher; // 설교자 이름

    @Column(name = "sermon_date", nullable = false)
    private LocalDate sermonDate; // 설교 날짜

    @Column(name = "bible_text", length = 100)
    private String bibleText; // 성경 본문 구절

    // 외부 API로 가져온 실제 구절 텍스트를 담기 위한 @Transient 필드
    @Transient
    private List<String> bibleVerses = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String content; // 설명

    // ✅ 공통 필드
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
