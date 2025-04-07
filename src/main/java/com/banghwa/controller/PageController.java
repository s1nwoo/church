package com.banghwa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/location")
    public String location() {
        return "location"; // → templates/location.html 렌더링됨
    }
}
