// src/main/java/com/banghwa/service/BibleLocalService.java
package com.banghwa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BibleLocalService {

    private Map<String, String> versesMap;

    @PostConstruct
    public void loadBible() throws Exception {
        ObjectMapper om = new ObjectMapper();
        try (InputStream is = new ClassPathResource("bible-kor.json").getInputStream()) {
            // JSON 구조: { "창1:1": "...", "창1:2": "...", ... }
            versesMap = om.readValue(is, new TypeReference<Map<String,String>>() {});
        }
    }

    /**
     * reference 예: "삼하6:1-10"
     * -> ["삼하6:1", "삼하6:2", ... ,"삼하6:10"]
     */
    public List<String> fetchVerses(String reference) {
        List<String> result = new ArrayList<>();
        try {
            String[] parts = reference.split(":");
            String bookChapter = parts[0];          // "삼하6"
            String[] range  = parts[1].split("-");
            int start = Integer.parseInt(range[0]);
            int end   = (range.length>1) ? Integer.parseInt(range[1]) : start;
            for (int v = start; v <= end; v++) {
                String key = bookChapter + ":" + v;
                String text = versesMap.get(key);
                if (text != null) {
                    result.add("[" + key + "] " + text);
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 빈 리스트 리턴
        }
        return result;
    }
}
