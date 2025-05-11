// src/main/java/com/banghwa/service/SermonVideoServiceImpl.java
package com.banghwa.service;

import com.banghwa.model.SermonVideo;
import com.banghwa.repository.SermonVideoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SermonVideoServiceImpl implements SermonVideoService {

    private final SermonVideoRepository sermonVideoRepository;
    private final BibleLocalService bibleLocalService;  // JSON 기반 성경구절 서비스

    /** [기능] 전체 + 검색 + 최신순 정렬 */
    @Override
    public Page<SermonVideo> searchSermons(String keyword, Pageable pageable, boolean includeDeleted) {
        return sermonVideoRepository.findAll((Specification<SermonVideo>) (root, query, cb) -> {
            Predicate p = includeDeleted
                    ? cb.conjunction()
                    : cb.equal(root.get("deleted"), false);

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                Predicate title    = cb.like(root.get("title"),     like);
                Predicate preacher = cb.like(root.get("preacher"), like);
                Predicate bible    = cb.like(root.get("bibleText"), like);
                p = cb.and(p, cb.or(title, preacher, bible));
            }
            return p;
        }, pageable);
    }

    /** [기능] 단건 조회 + JSON 파일에서 성경구절 자동 주입 */
    @Override
    public Optional<SermonVideo> getSermon(Long id) {
        return sermonVideoRepository.findById(id)
                .map(s -> {
                    // 기존 필드 그대로 유지
                    // …

                    // bible_text 컬럼 기반으로 bible-kor.json 에서 구절 파싱
                    List<String> verses = bibleLocalService.fetchVerses(s.getBibleText());
                    s.setBibleVerses(verses);
                    return s;
                });
    }

    /** [기능] 설교 등록 */
    @Override
    public SermonVideo createSermon(SermonVideo sermon) {
        return sermonVideoRepository.save(sermon);
    }

    /** [기능] 설교 수정 (soft-delete 포함) */
    @Override
    public SermonVideo updateSermon(Long id, SermonVideo updated) {
        SermonVideo original = sermonVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설교 ID"));

        // 기존 필드 업데이트
        original.setTitle(      updated.getTitle());
        original.setYoutubeUrl( updated.getYoutubeUrl());
        original.setPreacher(   updated.getPreacher());
        original.setSermonDate( updated.getSermonDate());
        original.setBibleText(  updated.getBibleText());
        original.setContent(    updated.getContent());

        // 삭제 여부 플래그 반영
        original.setDeleted(updated.getDeleted());
        if (Boolean.TRUE.equals(updated.getDeleted())) {
            original.setDeletedDate(java.time.LocalDateTime.now());
        } else {
            original.setDeletedDate(null);
        }

        return sermonVideoRepository.save(original);
    }

    /** [기능] 설교 삭제 (soft-delete) */
    @Override
    public void deleteSermon(Long id) {
        SermonVideo sermon = sermonVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설교 ID"));

        sermon.setDeleted(true);
        sermon.setDeletedDate(java.time.LocalDateTime.now());

        sermonVideoRepository.save(sermon);
    }
}
