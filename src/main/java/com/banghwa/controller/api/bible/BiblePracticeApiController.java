package com.banghwa.controller.api.bible;

import com.banghwa.service.BiblePracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bible-practice")
public class BiblePracticeApiController {

    private final BiblePracticeService biblePracticeService;

    public BiblePracticeApiController(BiblePracticeService biblePracticeService) {
        this.biblePracticeService = biblePracticeService;
    }

    @GetMapping("/books")
    public List<Map<String, String>> getBooks() {
        return biblePracticeService.getBooks();
    }

    @GetMapping("/chapters")
    public List<Map<String, Object>> getChapters(
            @RequestParam("bookCode") String bookCode
    ) {
        return biblePracticeService.getChaptersByBook(bookCode);
    }

    @GetMapping("/verses")
    public List<Map<String, Object>> getVerses(
            @RequestParam("bookCode") String bookCode,
            @RequestParam("chapter") int chapter
    ) {
        return biblePracticeService.getVerses(bookCode, chapter);
    }

    @GetMapping("/verse")
    public Map<String, Object> getVerse(
            @RequestParam("bookCode") String bookCode,
            @RequestParam("chapter") int chapter,
            @RequestParam("verse") int verse
    ) {
        return biblePracticeService.getVerse(bookCode, chapter, verse);
    }
}