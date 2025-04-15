package com.banghwa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class GameRainService {

    private final Map<String, String> verseMap = new HashMap<>();
    private final Random random = new Random();

    @PostConstruct
    public void loadBibleVerses() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = getClass().getResourceAsStream("/data/bible-kor.json");

            if (inputStream == null) {
                throw new IllegalStateException("bible-kor.json 파일을 찾을 수 없습니다.");
            }

            Map<String, String> loaded = mapper.readValue(inputStream, new TypeReference<>() {});
            verseMap.putAll(loaded);
        } catch (Exception e) {
            throw new RuntimeException("성경 데이터를 불러오는 중 오류 발생", e);
        }
    }

    public String getVerseByDifficulty(String level) {
        int minLength;
        int maxLength;

        switch (level) {
            case "easy" -> {
                minLength = 0;
                maxLength = 11;
            }
            case "normal" -> {
                minLength = 12;
                maxLength = 19;
            }
            case "hard" -> {
                minLength = 20;
                maxLength = Integer.MAX_VALUE;
            }
            default -> {
                minLength = 0;
                maxLength = 11;
            }
        }

        final int min = minLength;
        final int max = maxLength;

        List<String> verses = verseMap.values().stream()
                .filter(v -> v.length() >= min && v.length() <= max)
                .filter(v -> v.replaceAll("[^가-힣]", "").length() > 2) // 한글 최소 3자 이상
                .filter(v -> !v.contains("없음") && !v.equals("없음"))
                .toList();

        if (verses.isEmpty()) {
            return "해당 난이도의 성경 구절이 없습니다.";
        }

        return verses.get(random.nextInt(verses.size()));
    }

}
