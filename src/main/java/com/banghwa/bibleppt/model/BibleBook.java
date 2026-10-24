package com.banghwa.bibleppt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 성경 책 한 권을 표현하는 모델.
 *
 * <p>JSON 파일({@code bible-book-list.json})의 키는 한글("책이름", "약자")이지만,
 * 자바 코드 안에서는 영문 필드명을 쓰기 위해 {@link JsonProperty}로 매핑한다.
 * 이렇게 하면 데이터 파일 포맷과 코드가 분리되어 유지보수가 쉬워진다.</p>
 *
 * <p>불변 객체로 설계했다. 한 번 만들어진 책 정보는 바뀌지 않는다.</p>
 */
public final class BibleBook {

    /** 정식 책 이름 (예: "창세기"). */
    private final String name;

    /** 약자 (예: "창"). 구절 키 생성과 검색에 사용된다. */
    private final String abbreviation;

    /**
     * Jackson 역직렬화 및 일반 생성 모두에 쓰이는 생성자.
     *
     * @param name         정식 책 이름 (JSON 키 "책이름")
     * @param abbreviation 약자        (JSON 키 "약자")
     */
    public BibleBook(@JsonProperty("name") @com.fasterxml.jackson.annotation.JsonAlias("책이름") String name,
                     @JsonProperty("abbreviation") @com.fasterxml.jackson.annotation.JsonAlias({"abbr", "약자"}) String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    /** 검색어가 책 이름 또는 약자에 포함되는지 (대소문자 무시) 판단한다. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lower = query.toLowerCase();
        return name.toLowerCase().contains(lower)
                || abbreviation.toLowerCase().contains(lower);
    }

    /** ListView 등에 표시될 문자열. 예: "창세기 (창)". */
    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }
}
