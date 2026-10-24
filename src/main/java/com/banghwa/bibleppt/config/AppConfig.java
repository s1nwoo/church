package com.banghwa.bibleppt.config;

import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.io.InputStream;

/** Packaged resources work in both a development checkout and the deployed Spring Boot JAR. */
public final class AppConfig {
    public String getBookListPath() { return "data/bible-book-list.json"; }
    public String getVersesPath() { return "data/bible-kor.json"; }
    public String getTemplatePath() { return "bible-ppt/template.pptx"; }
    public String getTitlePlaceholder() { return "[TITLE]"; }
    public String getBodyPlaceholder() { return "[BODY]"; }
    public double getFontSize() { return 40; }
    public String getFontFamily() { return "Pretendard Black"; }
    public int getWrapWidth() { return 18; }
    public InputStream openTemplate() throws IOException {
        return new ClassPathResource(getTemplatePath()).getInputStream();
    }
}
