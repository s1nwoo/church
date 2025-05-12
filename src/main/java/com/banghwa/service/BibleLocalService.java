// src/main/java/com/banghwa/service/BibleLocalService.java
package com.banghwa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BibleLocalService {

    private Map<String, String> versesMap = Collections.emptyMap();

    /**
     * 애플리케이션 시작 시 한 번만 호출됩니다.
     * data/bible-kor.json 경로에서 JSON을 읽어 versesMap에 저장합니다.
     */
    @PostConstruct
    public void loadBible() {
        try (InputStream is = new ClassPathResource("data/bible-kor.json").getInputStream()) {
            ObjectMapper om = new ObjectMapper();
            // JSON 구조: { "창1:1": "...", "창1:2": "...", ... }
            versesMap = om.readValue(is, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            // 파일이 없거나 파싱 에러 시 로그만 남기고 빈 맵 유지
            System.err.println("bible-kor.json 로드 실패: " + e.getMessage());
            versesMap = Collections.emptyMap();
        }
    }

    /**
     * reference 예: "삼하6:1-10"
     * -> ["삼하6:1: 본문", ..., "삼하6:10: 본문"]
     */
    public List<String> fetchVerses(String reference) {
        if (versesMap.isEmpty() || reference == null || !reference.contains(":")) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        try {
            // 예: "삼하6:1-10" -> bookChap="삼하6", range="1-10"
            String[] parts = reference.split(":");
            String bookChap = parts[0];
            String[] range = parts[1].split("-");
            int start = Integer.parseInt(range[0].trim());
            int end   = (range.length > 1) ? Integer.parseInt(range[1].trim()) : start;

            for (int v = start; v <= end; v++) {
                String key = bookChap + ":" + v;
                String verseText = versesMap.get(key);
                if (verseText != null) {
                    result.add("[" + key + "] " + verseText);
                }
            }
        } catch (Exception e) {
            // 파싱 에러 시 빈 리스트 반환
        }
        return result;
    }
}
