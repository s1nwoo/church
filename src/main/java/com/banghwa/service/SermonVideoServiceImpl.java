package com.banghwa.service;

import com.banghwa.model.SermonVideo;
import com.banghwa.repository.SermonVideoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SermonVideoServiceImpl implements SermonVideoService {

    private final SermonVideoRepository sermonVideoRepository;

    // [기능] 전체 + 검색 + 최신순 정렬
    @Override
    public Page<SermonVideo> searchSermons(String keyword, Pageable pageable) {
        return sermonVideoRepository.findAll((Specification<SermonVideo>) (root, query, cb) -> {
            Predicate p = cb.equal(root.get("deleted"), false); // 삭제되지 않은 것만

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                Predicate title = cb.like(root.get("title"), like);
                Predicate preacher = cb.like(root.get("preacher"), like);
                Predicate bible = cb.like(root.get("bibleText"), like);
                p = cb.and(p, cb.or(title, preacher, bible));
            }

            return p;
        }, pageable);
    }

    @Override
    public Optional<SermonVideo> getSermon(Long id) {
        return sermonVideoRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()));
    }

    @Override
    public SermonVideo createSermon(SermonVideo sermon) {
        return sermonVideoRepository.save(sermon);
    }

    @Override
    public SermonVideo updateSermon(Long id, SermonVideo updated) {
        SermonVideo original = sermonVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설교 ID"));

        original.setTitle(updated.getTitle());
        original.setYoutubeUrl(updated.getYoutubeUrl());
        original.setPreacher(updated.getPreacher());
        original.setSermonDate(updated.getSermonDate());
        original.setBibleText(updated.getBibleText());
        original.setContent(updated.getContent());

        return sermonVideoRepository.save(original);
    }

    @Override
    public void deleteSermon(Long id) {
        SermonVideo sermon = sermonVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설교 ID"));

        sermon.setDeleted(true);
        sermon.setDeletedDate(java.time.LocalDateTime.now());

        sermonVideoRepository.save(sermon);
    }
}
