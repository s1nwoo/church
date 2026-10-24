package com.banghwa.bibleppt.parser;

import com.banghwa.bibleppt.data.BibleRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 입력한 구절 문자열을 실제 구절 키 목록으로 변환하는 파서.
 *
 * <p>지원 형식 (공백으로 여러 "구절 그룹"을 나열):</p>
 * <ul>
 *   <li>{@code 창1:1}          → 단일 절</li>
 *   <li>{@code 창1:1-5}        → 범위 ({@code -} 또는 {@code ~} 둘 다 가능: {@code 창1:1~5})</li>
 *   <li>{@code 창1:1,3,5}      → 콤마로 개별 절 나열</li>
 *   <li>{@code 창1:1-3,7,9-10} → 범위와 나열을 섞어서 사용</li>
 *   <li>{@code 창1:1-5 출1:1-2} → 공백으로 서로 다른 책/장 구절을 여러 개 나열</li>
 * </ul>
 *
 * <p>{@code ~}는 입력 단계에서 {@code -}로 정규화되므로, 사용자가 어느 쪽을 쓰든
 * 동일하게 동작한다. 해석된 각 키는 {@link BibleRepository}에 실제로 존재하는지까지
 * 검증하며, 형식이 잘못됐거나 데이터에 없는 토큰은 {@link ParseResult#invalidTokens()}에 모인다.</p>
 *
 * <p>확장 여지: 장 넘김({@code 창1:30-2:3})은 현재 미지원이며,
 * {@link #expandToken}만 확장하면 다른 코드 변경 없이 추가할 수 있다.</p>
 */
public final class ReferenceParser {

    private static final char CHAPTER_VERSE_SEPARATOR = ':';
    private static final char RANGE_SEPARATOR = '-';
    /** 사용자가 흔히 쓰는 대체 범위 기호. 정규화 단계에서 {@link #RANGE_SEPARATOR}로 치환된다. */
    private static final char RANGE_SEPARATOR_ALIAS = '~';
    private static final char LIST_SEPARATOR = ',';

    private final BibleRepository repository;

    public ReferenceParser(BibleRepository repository) {
        this.repository = repository;
    }

    /**
     * 입력 문자열 전체를 해석한다.
     *
     * @param input 사용자 입력 (예: "창1:1-5,9 출1:1~2")
     * @return 유효 키 목록과 무효 토큰 목록을 담은 결과
     */
    public ParseResult parse(String input) {
        List<String> validKeys = new ArrayList<>();
        List<String> invalidTokens = new ArrayList<>();

        if (input == null || input.isBlank()) {
            return new ParseResult(validKeys, invalidTokens);
        }

        for (String token : input.trim().split("\\s+")) {
            expandToken(token, validKeys, invalidTokens);
        }
        return new ParseResult(validKeys, invalidTokens);
    }

    /**
     * 토큰 하나(공백으로 구분된 한 덩어리)를 개별 구절 키로 펼쳐서 유효/무효 목록에 나눠 담는다.
     * 토큰은 "책약자+장:절부분" 구조여야 하며, 절부분은 콤마로 나뉜 여러 세그먼트를 가질 수 있고
     * 각 세그먼트는 단일 절이거나 범위일 수 있다.
     *
     * @param token         "창1:1", "창1:1-5", "창1:1,3,5-7" 등의 토큰
     * @param validKeys     (출력) 존재하는 구절 키가 추가됨
     * @param invalidTokens (출력) 형식 오류/미존재 토큰이 추가됨
     */
    private void expandToken(String token, List<String> validKeys, List<String> invalidTokens) {
        String normalized = token.replace(RANGE_SEPARATOR_ALIAS, RANGE_SEPARATOR);

        int colon = normalized.indexOf(CHAPTER_VERSE_SEPARATOR);
        if (colon < 0) {
            // "책약자+장:절" 구조 자체가 아니면 해석할 수 없다.
            invalidTokens.add(token);
            return;
        }

        String prefix = normalized.substring(0, colon);        // 예: "창1"
        String versePart = normalized.substring(colon + 1);    // 예: "1-3,7,9-10"

        for (String segment : versePart.split(String.valueOf(LIST_SEPARATOR), -1)) {
            expandSegment(prefix, segment.trim(), token, validKeys, invalidTokens);
        }
    }

    /**
     * 콤마로 나뉜 세그먼트 하나(단일 절 또는 범위)를 펼친다.
     *
     * @param prefix        "책약자+장" 부분 (예: "창1")
     * @param segment       "5" 또는 "1-3" 형태의 절 세그먼트
     * @param originalToken 오류 메시지에 표시할 원본 입력 토큰
     */
    private void expandSegment(String prefix, String segment, String originalToken,
                               List<String> validKeys, List<String> invalidTokens) {
        if (segment.isEmpty()) {
            invalidTokens.add(originalToken);
            return;
        }

        if (segment.indexOf(RANGE_SEPARATOR) < 0) {
            // 범위 기호가 없으면 단일 절.
            addVerseIfNumeric(prefix, segment, originalToken, validKeys, invalidTokens);
            return;
        }

        String[] bounds = segment.split(String.valueOf(RANGE_SEPARATOR), -1);
        if (bounds.length != 2) {
            invalidTokens.add(originalToken);
            return;
        }

        try {
            int start = Integer.parseInt(bounds[0].trim());
            int end = Integer.parseInt(bounds[1].trim());
            if (start < 1 || end > 176 || start > end) {
                invalidTokens.add(originalToken);
                return;
            }
            for (int verse = start; verse <= end; verse++) {
                String key = prefix + CHAPTER_VERSE_SEPARATOR + verse;
                // 범위 안의 개별 절 각각을 존재 여부로 검증한다.
                addKeyOrInvalid(key, key, validKeys, invalidTokens);
            }
        } catch (NumberFormatException e) {
            invalidTokens.add(originalToken);
        }
    }

    /** 절 번호가 숫자인지 확인하고, 숫자면 키를 만들어 존재 여부까지 검증한다. */
    private void addVerseIfNumeric(String prefix, String versePart, String originalToken,
                                   List<String> validKeys, List<String> invalidTokens) {
        try {
            int verse = Integer.parseInt(versePart.trim());
            String key = prefix + CHAPTER_VERSE_SEPARATOR + verse;
            addKeyOrInvalid(key, key, validKeys, invalidTokens);
        } catch (NumberFormatException e) {
            invalidTokens.add(originalToken);
        }
    }

    /** 키가 데이터에 있으면 유효 목록에, 없으면 무효 목록(표시용 라벨)에 추가한다. */
    private void addKeyOrInvalid(String key, String displayLabel,
                                 List<String> validKeys, List<String> invalidTokens) {
        if (repository.hasVerse(key)) {
            validKeys.add(key);
        } else {
            invalidTokens.add(displayLabel);
        }
    }
}
