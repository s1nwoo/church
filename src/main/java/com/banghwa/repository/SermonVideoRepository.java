package com.banghwa.repository;

import com.banghwa.model.SermonVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SermonVideoRepository extends JpaRepository<SermonVideo, Long>,
        JpaSpecificationExecutor<SermonVideo> {
    // 검색 조건을 위한 JpaSpecificationExecutor 추가
}
