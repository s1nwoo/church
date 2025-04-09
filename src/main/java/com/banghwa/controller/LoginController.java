package com.banghwa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // ✅ 이 클래스는 웹 페이지를 반환하는 컨트롤러임을 의미
public class LoginController {

    // ✅ 로그인 페이지 요청이 들어오면 login.html 뷰를 반환
    @GetMapping("/login")
    public String login() {
        // templates/login.html 을 반환
        return "login";
    }
}
