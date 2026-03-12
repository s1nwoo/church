package com.banghwa.repository;

import com.banghwa.model.SermonVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SermonVideoRepository extends JpaRepository<SermonVideo, Long>,
        JpaSpecificationExecutor<SermonVideo> {

    // 유튜브 URL 중복 체크용
    boolean existsByYoutubeUrl(String youtubeUrl);
}