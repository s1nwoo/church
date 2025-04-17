package com.banghwa.controller.api.bible;

import com.banghwa.model.BibleProgress;
import com.banghwa.model.User;
import com.banghwa.repository.UserRepository;
import com.banghwa.service.BiblePracticeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bible-practice")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class BiblePracticeApiController {

    private final BiblePracticeService biblePracticeService;
    private final UserRepository        userRepository;

    // 1) 책 목록 조회
    @GetMapping("/books")
    public ResponseEntity<List<Map<String, String>>> getBooks() {
        List<Map<String, String>> books = biblePracticeService.getBooks();
        return ResponseEntity.ok(books);
    }

    // 2) 장 목록 조회
    @GetMapping("/chapters")
    public ResponseEntity<List<Map<String, Object>>> getChapters(
            @RequestParam String bookCode
    ) {
        List<Map<String, Object>> chapters = biblePracticeService.getChaptersByBook(bookCode);
        return ResponseEntity.ok(chapters);
    }

    // 3) 절 목록 조회
    @GetMapping("/verses")
    public ResponseEntity<List<Map<String, Object>>> getVerses(
            @RequestParam String bookCode,
            @RequestParam int chapter
    ) {
        List<Map<String, Object>> verses = biblePracticeService.getVerses(bookCode, chapter);
        return ResponseEntity.ok(verses);
    }

    // 4) 특정 구절 조회
    @GetMapping("/verse")
    public ResponseEntity<Map<String, Object>> getVerse(
            @RequestParam String bookCode,
            @RequestParam int chapter,
            @RequestParam int verse
    ) {
        Map<String, Object> verseData = biblePracticeService.getVerse(bookCode, chapter, verse);
        return ResponseEntity.ok(verseData);
    }

    // 5) 진행 위치 저장/갱신
    @PostMapping("/progress")
    public Map<String, Object> saveProgress(
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));

        String bookCode = (String) payload.get("bookCode");
        int    chapter  = (Integer) payload.get("chapter");
        int    verse    = (Integer) payload.get("verse");

        biblePracticeService.saveOrUpdateProgress(user, bookCode, chapter, verse);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        return result;
    }

    // 6) 저장된 진행 위치 조회
    @GetMapping("/progress")
    public Map<String, Object> getProgress(
            @RequestParam String bookCode,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));

        Optional<BibleProgress> opt = biblePracticeService.getProgress(user, bookCode);

        Map<String, Object> result = new HashMap<>();
        if (opt.isPresent()) {
            BibleProgress p = opt.get();
            result.put("chapter", p.getChapter());
            result.put("verse",   p.getVerse());
        } else {
            result.put("chapter", 1);
            result.put("verse",   1);
        }
        return result;
    }
}
