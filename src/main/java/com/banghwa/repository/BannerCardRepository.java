package com.banghwa.repository;

import com.banghwa.model.BannerCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BannerCard Repository
 */
@Repository
public interface BannerCardRepository extends JpaRepository<BannerCard, Long> {

    /**
     * 활성 카드만 순서대로 조회 (프론트 슬라이더용)
     */
    List<BannerCard> findByActiveTrueOrderBySortOrderAsc();

    /**
     * 전체 카드 순서대로 조회 (관리자용)
     */
    List<BannerCard> findAllByOrderBySortOrderAsc();
}
