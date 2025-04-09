package com.banghwa.controller;

import com.banghwa.model.TypingForm;
import com.banghwa.service.BibleTypingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/reading")
public class BibleReadingController {

    @Autowired
    private BibleTypingService typingService;

    @GetMapping("")
    public String startTyping(@RequestParam(name = "book", required = false) String book, Model model) {
        if (book != null) {
            typingService.loadVersesByBook(book);
        }

        model.addAttribute("currentKey", typingService.getCurrentKey());
        model.addAttribute("verseText", typingService.getCurrentVerseText());
        model.addAttribute("progressPercent", typingService.getProgressPercent());
        model.addAttribute("typingForm", new TypingForm());
        model.addAttribute("currentIndex", typingService.getCurrentVerseIndex());     // ✅ 추가
        model.addAttribute("totalCount", typingService.getTotalVerseCount());        // ✅ 추가

        return "reading";
    }

    @PostMapping("/check")
    public String checkTyping(@ModelAttribute TypingForm typingForm, Model model) {
        boolean correct = typingService.checkTyping(typingForm.getTypedText());

        model.addAttribute("correct", correct);
        model.addAttribute("currentKey", typingService.getCurrentKey());
        model.addAttribute("verseText", typingService.getCurrentVerseText());
        model.addAttribute("typingForm", new TypingForm());
        model.addAttribute("progressPercent", typingService.getProgressPercent());
        model.addAttribute("remainingVerses", typingService.getRemainingVerseCount());

        return "reading";
    }

    @PostMapping("/next-ajax")
    @ResponseBody
    public Map<String, Object> goToNextVerseAjax() {
        typingService.moveToNextVerse();

        Map<String, Object> result = new HashMap<>();
        result.put("currentKey", typingService.getCurrentKey());
        result.put("verseText", typingService.getCurrentVerseText());
        result.put("progressPercent", typingService.getProgressPercent());
        result.put("remainingVerses", typingService.getRemainingVerseCount());
        result.put("currentIndex", typingService.getCurrentVerseIndex());
        result.put("totalCount", typingService.getTotalVerseCount());
        result.put("isFinished", typingService.isFinished()); // 👈 이거 boolean 타입으로 제대로
        return result;
    }
}
