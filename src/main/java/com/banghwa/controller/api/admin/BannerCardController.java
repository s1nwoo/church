package com.banghwa.controller.api.admin;

import com.banghwa.model.BannerCard;
import com.banghwa.repository.BannerCardRepository;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 메인 페이지 카드 슬라이더 관리 API
 *
 * 공개 API (인증 불필요):
 *   GET /api/banner-cards          - 활성 카드 목록 (슬라이더용)
 *
 * 관리자 API (ADMIN 권한 필요):
 *   GET    /api/admin/banner-cards          - 전체 카드 목록
 *   POST   /api/admin/banner-cards          - 카드 등록
 *   PUT    /api/admin/banner-cards/{id}     - 카드 수정
 *   DELETE /api/admin/banner-cards/{id}     - 카드 삭제
 *   PATCH  /api/admin/banner-cards/{id}/toggle - 활성/비활성 전환
 *   PATCH  /api/admin/banner-cards/reorder  - 순서 일괄 저장
 */
@RestController
@RequiredArgsConstructor
public class BannerCardController {

    private final BannerCardRepository bannerCardRepository;

    /* ============================================================
     * 공개 API - 활성 카드 목록 (CardSlider.js에서 호출)
     * ============================================================ */

    /**
     * 활성 카드만 순서대로 반환 (슬라이더 표시용)
     */
    @GetMapping("/api/banner-cards")
    public List<BannerCard> getActiveCards() {
        return bannerCardRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    /* ============================================================
     * 관리자 API
     * ============================================================ */

    /**
     * 전체 카드 목록 조회 (비활성 포함)
     */
    @GetMapping("/api/admin/banner-cards")
    @RolesAllowed("ADMIN")
    public List<BannerCard> getAllCards() {
        return bannerCardRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 카드 등록
     */
    @PostMapping("/api/admin/banner-cards")
    @RolesAllowed("ADMIN")
    public ResponseEntity<BannerCard> createCard(@RequestBody BannerCard card) {
        // sortOrder 미입력 시 마지막 순서로 자동 지정
        if (card.getSortOrder() == null || card.getSortOrder() == 0) {
            List<BannerCard> all = bannerCardRepository.findAllByOrderBySortOrderAsc();
            card.setSortOrder(all.isEmpty() ? 1 : all.get(all.size() - 1).getSortOrder() + 1);
        }
        BannerCard saved = bannerCardRepository.save(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 카드 수정
     */
    @PutMapping("/api/admin/banner-cards/{id}")
    @RolesAllowed("ADMIN")
    public ResponseEntity<BannerCard> updateCard(
            @PathVariable Long id,
            @RequestBody BannerCard req
    ) {
        Optional<BannerCard> opt = bannerCardRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BannerCard card = opt.get();
        card.setTitle(req.getTitle());
        card.setImageUrl(req.getImageUrl());
        card.setLinkUrl(req.getLinkUrl());
        card.setLinkType(req.getLinkType());
        card.setSortOrder(req.getSortOrder());
        card.setActive(req.getActive());
        return ResponseEntity.ok(bannerCardRepository.save(card));
    }

    /**
     * 카드 삭제 (완전 삭제 - 슬라이더 이미지이므로 soft delete 불필요)
     */
    @DeleteMapping("/api/admin/banner-cards/{id}")
    @RolesAllowed("ADMIN")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        if (!bannerCardRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bannerCardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 활성/비활성 전환 (토글)
     */
    @PatchMapping("/api/admin/banner-cards/{id}/toggle")
    @RolesAllowed("ADMIN")
    public ResponseEntity<BannerCard> toggleActive(@PathVariable Long id) {
        Optional<BannerCard> opt = bannerCardRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BannerCard card = opt.get();
        card.setActive(!card.getActive()); // 활성 ↔ 비활성 전환
        return ResponseEntity.ok(bannerCardRepository.save(card));
    }

    /**
     * 순서 일괄 저장
     * 요청 body: [{"id": 1, "sortOrder": 1}, {"id": 2, "sortOrder": 2}, ...]
     */
    @PatchMapping("/api/admin/banner-cards/reorder")
    @RolesAllowed("ADMIN")
    public ResponseEntity<List<BannerCard>> reorder(@RequestBody List<ReorderRequest> requests) {
        for (ReorderRequest req : requests) {
            bannerCardRepository.findById(req.getId()).ifPresent(card -> {
                card.setSortOrder(req.getSortOrder());
                bannerCardRepository.save(card);
            });
        }
        return ResponseEntity.ok(bannerCardRepository.findAllByOrderBySortOrderAsc());
    }

    /** 순서 변경 요청 DTO */
    public static class ReorderRequest {
        private Long id;
        private Integer sortOrder;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
