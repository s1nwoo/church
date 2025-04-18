package com.banghwa.service;

import com.banghwa.model.SermonVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SermonVideoService {

    // 영상 목록 조회 (검색 + 페이지네이션 + 정렬 포함)
    Page<SermonVideo> searchSermons(String keyword, Pageable pageable);

    // 영상 단건 조회
    Optional<SermonVideo> getSermon(Long id);

    // 영상 등록
    SermonVideo createSermon(SermonVideo sermon);

    // 영상 수정
    SermonVideo updateSermon(Long id, SermonVideo updated);

    // 영상 삭제 (소프트 딜리트)
    void deleteSermon(Long id);
}
