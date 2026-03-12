package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메인 페이지 카드 슬라이더 항목 엔티티
 * - 관리자 페이지에서 CRUD 가능
 * - sortOrder로 순서 조정
 * - active로 표시/숨김 전환
 */
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "banner_card")
public class BannerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카드 제목 (관리자용 구분 이름) */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 이미지 경로
     * - 로컬 이미지: "card/card1.png" (public 폴더 기준)
     * - 외부 URL:    "https://..." 형태
     */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * 클릭 시 이동할 링크
     * - 내부 경로: "/church-intro"
     * - 외부 URL:  "https://www.youtube.com/..."
     * - 없음:      null 또는 빈 문자열
     */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * 링크 타입
     * - "internal": React Router navigate (내부 이동)
     * - "external": window.open (새 탭)
     * - "none": 클릭 무반응
     */
    @Column(name = "link_type", length = 20)
    @Builder.Default
    private String linkType = "none";

    /** 표시 순서 (오름차순) */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** 활성 여부 (false면 슬라이더에서 제외) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
