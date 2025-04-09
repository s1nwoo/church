package com.banghwa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * 성경 타자 연습을 위한 서비스 클래스
 * - 성경책 목록 관리
 * - 각 구절 관리
 * - 진행률 및 현재 상태 추적
 */
@Service
public class BibleTypingService {

    // ✅ 전체 성경 구절 (초기 로딩된 원본)
    private final Map<String, String> fullBible = new LinkedHashMap<>();

    // ✅ 현재 선택된 성경책의 구절 목록
    private final Map<String, String> bibleVerses = new LinkedHashMap<>();

    // ✅ 성경책 리스트 (book + abbr)
    private final List<Map<String, String>> bibleBooks = new ArrayList<>();

    // ✅ 현재 절 위치 정보
    private List<String> verseKeys = new ArrayList<>();
    private String currentKey;
    private int currentIndex = 0;
    private String selectedBook = null;

    // ✅ 전체 성경 JSON 로드 (앱 시작 시 자동 실행)
    @PostConstruct
    public void initBibleData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = getClass().getResourceAsStream("/data/bible-kor.json");

            if (inputStream == null) {
                throw new IllegalStateException("bible-kor.json 파일을 찾을 수 없습니다.");
            }

            Map<String, String> loaded = mapper.readValue(inputStream, new TypeReference<>() {});
            fullBible.putAll(loaded);              // 전체 성경 저장
            bibleVerses.putAll(loaded);            // 초기값은 전체
            verseKeys = new ArrayList<>(bibleVerses.keySet());
            currentKey = verseKeys.get(0);
        } catch (Exception e) {
            throw new RuntimeException("성경 데이터를 불러오는 중 오류 발생", e);
        }
    }

    // ✅ 성경책 목록 JSON 로드
    @PostConstruct
    public void loadBibleBookList() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = getClass().getResourceAsStream("/data/bible-book-list.json");

            if (inputStream == null) {
                throw new IllegalStateException("bible-book-list.json 파일을 찾을 수 없습니다.");
            }

            List<Map<String, String>> loaded = mapper.readValue(inputStream, new TypeReference<>() {});
            bibleBooks.addAll(loaded);
        } catch (Exception e) {
            throw new RuntimeException("성경책 목록 로딩 실패", e);
        }
    }

    // ✅ 사용자가 성경책 선택 시 구절 필터링
    public void loadVersesByBook(String bookAbbr) {
        selectedBook = bookAbbr;
        bibleVerses.clear();
        verseKeys.clear();
        currentIndex = 0;

        for (Map.Entry<String, String> entry : fullBible.entrySet()) {
            if (entry.getKey().startsWith(bookAbbr)) {
                bibleVerses.put(entry.getKey(), entry.getValue());
            }
        }

        verseKeys = new ArrayList<>(bibleVerses.keySet());
        currentKey = verseKeys.isEmpty() ? null : verseKeys.get(0);
    }

    // ✅ 현재 키(창1:1 등)
    public String getCurrentKey() {
        return currentKey;
    }

    // ✅ 현재 절 내용
    public String getCurrentVerseText() {
        return bibleVerses.getOrDefault(currentKey, "");
    }

    // ✅ 입력 검증
    public boolean checkTyping(String userInput) {
        String original = getCurrentVerseText();
        return original.trim().equals(userInput.trim());
    }

    // ✅ 다음 절로 이동
    public void moveToNextVerse() {
        if (currentIndex + 1 < verseKeys.size()) {
            currentIndex++;
            currentKey = verseKeys.get(currentIndex);
        }
    }

    // ✅ 전체 진행률 반환 (절 기준)
    public int getProgressPercent() {
        if (verseKeys.isEmpty()) return 0;
        return (int) (((double)(currentIndex + 1) / verseKeys.size()) * 100);
    }

    public int getRemainingVerses() {
        return verseKeys.size() - currentIndex - 1;
    }

    public boolean isFinished() {
        return currentIndex >= verseKeys.size() - 1;
    }

    // ✅ 성경책 목록 반환
    public List<Map<String, String>> getBibleBookList() {
        return bibleBooks;
    }

    // ✅ 현재 선택된 책의 약어
    public String getSelectedBook() {
        return selectedBook;
    }

    // ✅ 남은 절 수 반환
    public int getRemainingVerseCount() {
        return verseKeys.size() - (currentIndex + 1);
    }

    public int getCurrentVerseIndex() {
        return currentIndex + 1; // 사용자 기준으로 1부터 시작
    }

    public int getTotalVerseCount() {
        return verseKeys.size();
    }

}
