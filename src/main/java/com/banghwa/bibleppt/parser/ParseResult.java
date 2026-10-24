package com.banghwa.bibleppt.parser;

import java.util.Collections;
import java.util.List;

/**
 * 사용자가 입력한 구절 문자열을 해석한 결과.
 *
 * <p>유효하게 펼쳐진 구절 키 목록과, 해석에 실패했거나 데이터에 존재하지 않는
 * 토큰 목록을 함께 담는다. UI는 이 결과로 실시간 검증(빨간 표시)과 미리보기를
 * 동시에 처리할 수 있다.</p>
 *
 * @param keys          정상적으로 펼쳐진 구절 키들 (예: [창1:1, 창1:2, ...]), 입력 순서 유지
 * @param invalidTokens 형식이 잘못됐거나 데이터에 없는 입력 토큰들
 */
public record ParseResult(List<String> keys, List<String> invalidTokens) {

    public ParseResult {
        keys = Collections.unmodifiableList(keys);
        invalidTokens = Collections.unmodifiableList(invalidTokens);
    }

    /** 입력이 아예 비어 있는가(공백만 있는가). */
    public boolean isEmpty() {
        return keys.isEmpty() && invalidTokens.isEmpty();
    }

    /** 잘못된 토큰이 하나도 없고, 최소 한 개 이상의 구절이 있는가. */
    public boolean isValid() {
        return invalidTokens.isEmpty() && !keys.isEmpty();
    }

    /** 화면에 표시할 검증 메시지. 상황에 맞는 안내 문구를 돌려준다. */
    public String validationMessage() {
        if (isEmpty()) {
            return "";
        }
        if (!invalidTokens.isEmpty()) {
            return "확인이 필요한 구절: " + String.join(", ", invalidTokens);
        }
        return keys.size() + "개 구절 · 미리보기를 확인하세요.";
    }
}
