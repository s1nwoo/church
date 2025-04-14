package com.banghwa.controller;


import com.banghwa.model.Post;                    // Post 객체
import com.banghwa.service.CommunityService;      // 서비스 계층

import lombok.RequiredArgsConstructor;            // 생성자 자동 생성 어노테이션

import org.springframework.stereotype.Controller; // @Controller 어노테이션
import org.springframework.web.bind.annotation.GetMapping; // GET 요청 처리
import org.springframework.web.bind.annotation.PostMapping; // POST 요청 처리
import org.springframework.web.bind.annotation.ModelAttribute; // 폼 데이터 바인딩

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 리다이렉트 시 flash 메시지 전달
import org.springframework.ui.Model;
import java.util.List;

@Controller
@RequestMapping("community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService; // ★ 이게 꼭 있어야 해!

    @GetMapping("")
    public String list(Model model) {
        List<Post> posts = communityService.getAllPosts();
        model.addAttribute("posts", posts);
        return "community/list";
    }

    @GetMapping("/write")
    public String writeForm() {
        return "community/write";
    }

    @PostMapping("/write")
    public String submitPost(@ModelAttribute Post post, RedirectAttributes redirectAttributes) {
        communityService.savePost(post);
        redirectAttributes.addFlashAttribute("message", "글이 성공적으로 등록되었습니다!");
        return "redirect:/community"; // 다시 목록으로 이동
    }

    @PostMapping("/submit")
    public String submitPost(@ModelAttribute Post post) {
        communityService.savePost(post);
        return "redirect:/community";
    }

}
