package com.banghwa.bibleppt.model;

/**
 * PPT 슬라이드 한 장에 들어갈 내용(제목 + 본문)을 표현하는 불변 값 객체.
 *
 * <p>이 객체는 "미리보기"와 "실제 PPT 생성"이 공유한다. 즉, 화면에서 미리 본 슬라이드
 * 목록과 실제로 파일에 쓰이는 슬라이드가 완전히 동일함을 보장한다.</p>
 *
 * @param title 슬라이드 제목 (예: "창1:1") — 템플릿의 titlePlaceholder 자리에 들어간다
 * @param body  슬라이드 본문 (여러 줄일 수 있음)     — 템플릿의 bodyPlaceholder 자리에 들어간다
 */
public record SlideContent(String title, String body) {
}
