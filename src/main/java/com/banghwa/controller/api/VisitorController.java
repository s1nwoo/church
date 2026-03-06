// src/main/java/com/banghwa/controller/api/VisitorController.java
package com.banghwa.controller.api;

import com.banghwa.model.VisitorCount;
import com.banghwa.repository.VisitorCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/visitor")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorCountRepository visitorCountRepository;

    /**
     * [기능] 방문자 카운트 증가 + 오늘/전체 방문자 수 반환
     * - 페이지 로드 시 프론트엔드에서 호출
     * - 오늘 날짜 레코드가 없으면 새로 생성
     * - 있으면 dailyCount +1
     */
    @PostMapping("/count")
    public ResponseEntity<Map<String, Long>> countVisitor() {
        LocalDate today = LocalDate.now();

        // 가장 최근 누적 수 조회
        Optional<VisitorCount> latestOpt = visitorCountRepository.findTopByOrderByVisitDateDesc();
        long currentTotal = latestOpt.map(VisitorCount::getTotalCount).orElse(0L);

        // 오늘 레코드 조회
        Optional<VisitorCount> todayOpt = visitorCountRepository.findByVisitDate(today);

        VisitorCount record;
        if (todayOpt.isPresent()) {
            // 오늘 레코드가 이미 있으면 dailyCount +1
            record = todayOpt.get();
            record.setDailyCount(record.getDailyCount() + 1);
            record.setTotalCount(record.getTotalCount() + 1);
        } else {
            // 오늘 레코드가 없으면 새로 생성
            record = VisitorCount.builder()
                    .visitDate(today)
                    .dailyCount(1L)
                    .totalCount(currentTotal + 1)
                    .build();
        }

        visitorCountRepository.save(record);

        // 오늘 방문자 수 + 전체 누적 수 반환
        Map<String, Long> result = new HashMap<>();
        result.put("today", record.getDailyCount());
        result.put("total", record.getTotalCount());

        return ResponseEntity.ok(result);
    }

    /**
     * [기능] 방문자 수 조회 (카운트 증가 없이)
     * - 관리자 대시보드 등에서 활용 가능
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getVisitorCount() {
        LocalDate today = LocalDate.now();

        long todayCount = visitorCountRepository.findByVisitDate(today)
                .map(VisitorCount::getDailyCount)
                .orElse(0L);

        long totalCount = visitorCountRepository.findTopByOrderByVisitDateDesc()
                .map(VisitorCount::getTotalCount)
                .orElse(0L);

        Map<String, Long> result = new HashMap<>();
        result.put("today", todayCount);
        result.put("total", totalCount);

        return ResponseEntity.ok(result);
    }
}
