package com.banghwa.bibleppt.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 구절 키("창1:1")를 책 약자·장·절로 분해한 값.
 *
 * <p>여러 곳(저장소 색인, 슬라이드 결합 로직)에서 같은 키 형식을 다뤄야 해서 파싱 규칙을
 * 이 한 곳에 모아둔다.</p>
 *
 * @param abbreviation 책 약자 (예: "창")
 * @param chapter      장 번호
 * @param verse        절 번호
 */
public record VerseReference(String abbreviation, int chapter, int verse) {

    /** 구절 키 형식. 약자는 숫자를 포함하지 않는다고 가정한다. */
    private static final Pattern KEY_PATTERN = Pattern.compile("^(\\D+)(\\d+):(\\d+)$");

    /**
     * 구절 키를 분해한다. 형식이 맞지 않으면 빈 Optional을 돌려준다.
     *
     * @param key "창1:1" 형식의 구절 키
     */
    public static Optional<VerseReference> parse(String key) {
        Matcher matcher = KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new VerseReference(
                matcher.group(1),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    /** "책약자+장" 부분만. 예: "창1". */
    public String bookChapterPrefix() {
        return abbreviation + chapter;
    }

    /** 이 값을 다시 구절 키 문자열로 만든다. 예: "창1:1". */
    public String toKey() {
        return abbreviation + chapter + ":" + verse;
    }
}
