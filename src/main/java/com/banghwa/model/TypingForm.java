package com.banghwa.model;

import lombok.Data;

// ✅ 사용자가 입력한 텍스트를 담는 Form 객체
@Data
public class TypingForm {
    private String typedText;
}
