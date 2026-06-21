package com.banghwa.repository;

import com.banghwa.model.SermonVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface SermonVideoRepository extends JpaRepository<SermonVideo, Long>,
        JpaSpecificationExecutor<SermonVideo> {

    // YouTube URL duplicate check.
    boolean existsByYoutubeUrl(String youtubeUrl);

    boolean existsBySermonDateAndDeletedFalse(LocalDate sermonDate);
}
