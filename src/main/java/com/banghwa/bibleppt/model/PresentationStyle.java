package com.banghwa.bibleppt.model;

import java.awt.geom.Rectangle2D;

/**
 * 슬라이드에 적용할 제목·본문 글꼴 크기와, 제목·본문 상자의 위치/크기(포인트 단위).
 *
 * <p>템플릿의 기본값을 세션 중 사용자가 미리보기에서 조정한 값으로 대체할 때 쓰인다.
 * 미리보기 렌더링과 최종 PPT 생성이 항상 이 값을 함께 사용하므로, 미리보기에서 본 모습
 * 그대로 저장된다.</p>
 *
 * @param titleFontSize 제목에 적용할 글꼴 크기(pt)
 * @param bodyFontSize  본문에 적용할 글꼴 크기(pt)
 * @param titleAnchor   제목 상자의 위치·크기 (포인트 좌표계, PowerPoint 도형 anchor와 동일)
 * @param bodyAnchor    본문 상자의 위치·크기
 */
public record PresentationStyle(double titleFontSize, double bodyFontSize,
                                Rectangle2D titleAnchor, Rectangle2D bodyAnchor) {

    /** 제목 상자 위치만 바꾼 새 스타일을 돌려준다. */
    public PresentationStyle withTitleAnchor(Rectangle2D newTitleAnchor) {
        return new PresentationStyle(titleFontSize, bodyFontSize, newTitleAnchor, bodyAnchor);
    }

    /** 본문 상자 위치만 바꾼 새 스타일을 돌려준다. */
    public PresentationStyle withBodyAnchor(Rectangle2D newBodyAnchor) {
        return new PresentationStyle(titleFontSize, bodyFontSize, titleAnchor, newBodyAnchor);
    }

    /** 제목 글꼴 크기만 바꾼 새 스타일을 돌려준다. */
    public PresentationStyle withTitleFontSize(double newTitleFontSize) {
        return new PresentationStyle(newTitleFontSize, bodyFontSize, titleAnchor, bodyAnchor);
    }

    /** 본문 글꼴 크기만 바꾼 새 스타일을 돌려준다. */
    public PresentationStyle withBodyFontSize(double newBodyFontSize) {
        return new PresentationStyle(titleFontSize, newBodyFontSize, titleAnchor, bodyAnchor);
    }
}
