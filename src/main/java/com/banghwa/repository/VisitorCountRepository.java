// src/main/java/com/banghwa/repository/VisitorCountRepository.java
package com.banghwa.repository;

import com.banghwa.model.VisitorCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface VisitorCountRepository extends JpaRepository<VisitorCount, Long> {

    // 오늘 날짜 레코드 조회
    Optional<VisitorCount> findByVisitDate(LocalDate visitDate);

    // 가장 최근 레코드 조회 (전체 누적 수 확인용)
    Optional<VisitorCount> findTopByOrderByVisitDateDesc();
}
