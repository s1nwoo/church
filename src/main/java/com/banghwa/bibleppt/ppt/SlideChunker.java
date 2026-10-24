package com.banghwa.bibleppt.ppt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 긴 본문을 "슬라이드당 최대 줄 수"에 맞춰 여러 조각(chunk)으로 나누는 순수 함수 유틸리티.
 *
 * <p>상태를 갖지 않으며 부작용도 없으므로 테스트하기 쉽고 재사용이 안전하다.
 * 두 단계로 동작한다:</p>
 * <ol>
 *   <li><b>줄바꿈</b>: 단어 단위로 이어 붙이다가 {@code wrapWidth}(글자 폭)를 넘으면 줄을 바꾼다.</li>
 *   <li><b>분할</b>: 그렇게 만든 줄들을 {@code lineLimit}개씩 묶어 하나의 슬라이드 본문으로 만든다.</li>
 * </ol>
 */
public final class SlideChunker {

    /** 줄 사이 구분자. PPT 본문에서 줄바꿈으로 렌더링된다. */
    private static final String LINE_BREAK = "\n";

    private SlideChunker() {
        // 인스턴스화 방지: 정적 유틸리티 클래스.
    }

    /**
     * 본문을 슬라이드 본문 조각들로 나눈다.
     *
     * @param text      원본 본문
     * @param lineLimit 슬라이드 한 장에 담을 최대 줄 수. 0 이하이면 나누지 않고 통째로 반환.
     * @param wrapWidth 한 줄에 담을 최대 글자 폭
     * @return 슬라이드별 본문 문자열 목록 (각 원소가 슬라이드 한 장의 본문)
     */
    public static List<String> chunk(String text, int lineLimit, int wrapWidth) {
        if (lineLimit <= 0) {
            // 줄 수 제한이 없으면 원본을 그대로 한 장에 담는다.
            return Collections.singletonList(text);
        }

        List<String> lines = wrapIntoLines(text, wrapWidth);
        return groupLinesIntoSlides(lines, lineLimit);
    }

    /** 1단계: 단어를 이어 붙이며 wrapWidth 기준으로 줄을 나눈다. */
    private static List<String> wrapIntoLines(String text, int wrapWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split("\\s+")) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= wrapWidth) {
                currentLine.append(' ').append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /** 2단계: 줄들을 lineLimit개씩 묶어 슬라이드 본문으로 합친다. */
    private static List<String> groupLinesIntoSlides(List<String> lines, int lineLimit) {
        List<String> slides = new ArrayList<>();
        StringBuilder slide = new StringBuilder();
        int lineCount = 0;

        for (String line : lines) {
            if (lineCount > 0) {
                slide.append(LINE_BREAK);
            }
            slide.append(line);
            if (++lineCount == lineLimit) {
                slides.add(slide.toString());
                slide = new StringBuilder();
                lineCount = 0;
            }
        }
        if (slide.length() > 0) {
            slides.add(slide.toString());
        }
        return slides;
    }
}
