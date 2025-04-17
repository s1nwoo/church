package com.banghwa.service;

import com.banghwa.repository.BibleProgressRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.banghwa.model.BibleProgress;
import com.banghwa.model.User;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BiblePracticeService {

    private final BibleProgressRepository bibleProgressRepository;
    private Map<String, String> bibleVerses;
    private List<Map<String, String>> bibleBooks;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // bible-kor.json
            InputStream versesInput = new ClassPathResource("data/bible-kor.json").getInputStream();
            bibleVerses = objectMapper.readValue(versesInput, new TypeReference<>() {});

            // bible-book-list.json
            InputStream booksInput = new ClassPathResource("data/bible-book-list.json").getInputStream();
            bibleBooks = objectMapper.readValue(booksInput, new TypeReference<>() {});

        } catch (Exception e) {
            throw new RuntimeException("성경 데이터를 불러오는 중 오류 발생", e);
        }
    }

    public List<Map<String, String>> getBooks() {
        return bibleBooks;
    }

    public List<Map<String, Object>> getChaptersByBook(String bookCode) {
        List<Map<String, Object>> chapters = new ArrayList<>();
        Set<Integer> chapterSet = new TreeSet<>();

        for (String key : bibleVerses.keySet()) {
            if (key.startsWith(bookCode)) {
                String chapterStr = key.replace(bookCode, "").split(":")[0];
                chapterSet.add(Integer.parseInt(chapterStr));
            }
        }

        for (Integer chapter : chapterSet) {
            Map<String, Object> item = new HashMap<>();
            item.put("chapter", chapter);
            chapters.add(item);
        }

        return chapters;
    }

    public List<Map<String, Object>> getVerses(String bookCode, int chapter) {
        List<Map<String, Object>> verses = new ArrayList<>();

        for (Map.Entry<String, String> entry : bibleVerses.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(bookCode + chapter + ":")) {
                String verseNumStr = key.split(":")[1];
                int verseNum = Integer.parseInt(verseNumStr);

                Map<String, Object> verseData = new HashMap<>();
                verseData.put("verse", verseNum);
                verseData.put("text", entry.getValue());
                verses.add(verseData);
            }
        }

        return verses;
    }

    public Map<String, Object> getVerse(String bookCode, int chapter, int verse) {
        String key = bookCode + chapter + ":" + verse;
        String text = bibleVerses.getOrDefault(key, "(내용 없음)");

        Map<String, Object> result = new HashMap<>();
        result.put("verse", verse);
        result.put("text", text);
        return result;
    }

    // [기능] 성경 연습 위치 저장 또는 갱신
    // [설명] 사용자가 특정 책에서 마지막으로 완료한 장/절을 기록
    public void saveOrUpdateProgress(User user, String bookCode, int chapter, int verse) {
        Optional<BibleProgress> optional = bibleProgressRepository.findByUserAndBookCode(user, bookCode);

        BibleProgress progress = optional.orElse(
                BibleProgress.builder()
                        .user(user)
                        .bookCode(bookCode)
                        .build()
        );

        progress.setChapter(chapter);
        progress.setVerse(verse);

        bibleProgressRepository.save(progress);
    }

    // [기능] 저장된 성경 연습 위치 불러오기
    // [설명] 사용자가 특정 책에서 어디까지 연습했는지 반환
    public Optional<BibleProgress> getProgress(User user, String bookCode) {
        return bibleProgressRepository.findByUserAndBookCode(user, bookCode);
    }

}