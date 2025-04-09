package com.banghwa.controller;

import com.banghwa.service.BibleTypingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BibleListController {

    @Autowired
    private BibleTypingService typingService;

    @GetMapping("/bible")
    public String showBibleList(Model model) {
        model.addAttribute("bookList", typingService.getBibleBookList());
        return "bible_list";
    }
}
