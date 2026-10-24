package com.banghwa.bibleppt.data;

import com.banghwa.bibleppt.config.AppConfig;
import com.banghwa.bibleppt.model.BibleBook;
import com.banghwa.bibleppt.model.VerseReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 성경 데이터(책 목록 · 구절 본문)의 로딩과 조회를 책임지는 저장소.
 *
 * <p>데이터 접근을 이 한 클래스로 캡슐화한다. UI나 생성 로직은 "구절 키로 본문을 얻는다"는
 * 관심사만 알면 되고, 그 데이터가 JSON에서 오는지 DB에서 오는지는 몰라도 된다.
 * (향후 다른 번역본이나 저장 방식으로 확장할 때 이 클래스만 바꾸면 된다.)</p>
 *
 * <p>구절 맵의 키는 "약자+장:절" 형식이다. 예: {@code "창1:1"} → 본문 문자열.</p>
 *
 * <p>이 클래스는 실제 데이터로부터 "이 책에 어떤 장이 있는지", "이 장에 어떤 절이 있는지"를
 * 미리 색인해두고 {@link #getChapters}, {@link #getVerses}로 제공한다. 콤보박스처럼
 * "존재하는 값만 선택지로 보여줘야 하는" UI가 이 색인을 이용하면 애초에 없는 장·절을
 * 선택할 수 없게 되어 입력 오류가 원천적으로 발생하지 않는다.</p>
 */
public final class BibleRepository {

    private final List<BibleBook> books;
    private final Map<String, String> verses;
    /** 약자 → (장 → 그 장에 실제로 존재하는 절 번호 목록) 색인. */
    private final Map<String, TreeMap<Integer, List<Integer>>> chapterVerseIndex;

    private BibleRepository(List<BibleBook> books, Map<String, String> verses) {
        // 외부에서 목록을 바꾸지 못하도록 방어적으로 감싼다.
        this.books = Collections.unmodifiableList(books);
        this.verses = Collections.unmodifiableMap(verses);
        this.chapterVerseIndex = buildChapterVerseIndex(verses);
    }

    /**
     * 설정에 지정된 경로에서 책 목록과 구절 데이터를 읽어 저장소를 만든다.
     *
     * @param config 데이터 파일 경로가 담긴 설정
     * @return 로드된 저장소
     * @throws UncheckedIOException 데이터 파일을 읽지 못한 경우
     */
    public static BibleRepository load(AppConfig config) {
        List<BibleBook> books = readJson(
                config.getBookListPath(),
                new TypeReference<List<BibleBook>>() {});
        Map<String, String> verses = readJson(
                config.getVersesPath(),
                new TypeReference<Map<String, String>>() {});
        return new BibleRepository(books, verses);
    }

    /** 전체 책 목록 (읽기 전용). */
    public List<BibleBook> getBooks() {
        return books;
    }

    /**
     * 구절 키가 데이터에 존재하는지 확인한다.
     *
     * @param verseKey "창1:1" 형식의 키
     */
    public boolean hasVerse(String verseKey) {
        return verses.containsKey(verseKey);
    }

    /**
     * 구절 키에 해당하는 본문을 돌려준다. 없으면 {@code null}.
     *
     * @param verseKey "창1:1" 형식의 키
     */
    public String getVerse(String verseKey) {
        return verses.get(verseKey);
    }

    /**
     * 해당 책에 실제로 존재하는 장 번호 목록을 오름차순으로 돌려준다.
     * 데이터에 없는 약자면 빈 목록을 돌려준다.
     *
     * @param abbreviation 책 약자 (예: "창")
     */
    public List<Integer> getChapters(String abbreviation) {
        TreeMap<Integer, List<Integer>> chapters = chapterVerseIndex.get(abbreviation);
        return chapters == null ? List.of() : List.copyOf(chapters.keySet());
    }

    /**
     * 해당 책·장에 실제로 존재하는 절 번호 목록을 오름차순으로 돌려준다.
     * 데이터에 없는 책이나 장이면 빈 목록을 돌려준다.
     *
     * @param abbreviation 책 약자 (예: "창")
     * @param chapter      장 번호
     */
    public List<Integer> getVerses(String abbreviation, int chapter) {
        TreeMap<Integer, List<Integer>> chapters = chapterVerseIndex.get(abbreviation);
        if (chapters == null) {
            return List.of();
        }
        return chapters.getOrDefault(chapter, List.of());
    }

    /** 구절 맵의 모든 키를 스캔해 "약자 → 장 → 절 목록" 색인을 만든다. */
    private static Map<String, TreeMap<Integer, List<Integer>>> buildChapterVerseIndex(Map<String, String> verses) {
        Map<String, TreeMap<Integer, TreeSet<Integer>>> working = new HashMap<>();

        for (String key : verses.keySet()) {
            Optional<VerseReference> reference = VerseReference.parse(key);
            if (reference.isEmpty()) {
                continue; // 형식이 다른 키는 색인 대상에서 제외 (방어적)
            }
            VerseReference ref = reference.get();
            working.computeIfAbsent(ref.abbreviation(), k -> new TreeMap<>())
                    .computeIfAbsent(ref.chapter(), k -> new TreeSet<>())
                    .add(ref.verse());
        }

        // TreeSet -> 불변 List로 변환하며 최종 색인을 완성한다.
        Map<String, TreeMap<Integer, List<Integer>>> result = new HashMap<>();
        for (Map.Entry<String, TreeMap<Integer, TreeSet<Integer>>> bookEntry : working.entrySet()) {
            TreeMap<Integer, List<Integer>> chapters = new TreeMap<>();
            for (Map.Entry<Integer, TreeSet<Integer>> chapterEntry : bookEntry.getValue().entrySet()) {
                chapters.put(chapterEntry.getKey(), List.copyOf(chapterEntry.getValue()));
            }
            result.put(bookEntry.getKey(), chapters);
        }
        return result;
    }

    /** 제네릭 JSON 로딩 헬퍼. 실패 시 명확한 메시지와 함께 예외를 던진다. */
    private static <T> T readJson(String path, TypeReference<T> type) {
        try (var in = new org.springframework.core.io.ClassPathResource(path).getInputStream()) {
            return new ObjectMapper().readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "데이터 파일을 읽지 못했습니다: " + path, e);
        }
    }
}
