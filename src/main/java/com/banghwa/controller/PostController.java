package com.banghwa.controller;

import com.banghwa.model.Post;
import com.banghwa.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // [기능] 게시글 목록 화면
    // [설명] 게시글 목록을 조회해 모델에 담고 list.html로 이동한다
    @GetMapping("")
    public String list(Model model) {
        List<Post> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);
        return "posts/list";
    }

    // [기능] 게시글 작성 폼 화면
    // [설명] 글쓰기 화면으로 이동한다
    @GetMapping("/new")
    public String writeForm(Model model) {
        model.addAttribute("post", new Post());
        return "posts/write";
    }

    // [기능] 게시글 등록 처리
    // [설명] 작성된 게시글 데이터를 저장한 후 목록으로 리다이렉트한다
    @PostMapping("")
    public String submitPost(@ModelAttribute Post post, RedirectAttributes redirectAttributes) {
        postService.savePost(post);
        redirectAttributes.addFlashAttribute("message", "글이 성공적으로 등록되었습니다!");
        return "redirect:/posts";
    }

    // [기능] 게시글 상세 보기
    // [설명] 게시글 ID로 단건 조회하여 상세 페이지로 이동한다
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/posts";
        }
        model.addAttribute("post", post);
        return "posts/detail";
    }

    // [기능] 게시글 수정 폼 화면
    // [설명] 게시글 ID로 조회 후 수정화면으로 이동한다
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/posts";
        }
        model.addAttribute("post", post);
        return "posts/edit";
    }

    // [기능] 게시글 수정 처리
    // [설명] 수정된 내용을 반영하고 목록으로 리다이렉트한다
    @PostMapping("/{id}")
    public String updatePost(@PathVariable Long id, @ModelAttribute Post post, RedirectAttributes redirectAttributes) {
        boolean updated = postService.updatePost(id, post);
        if (updated) {
            redirectAttributes.addFlashAttribute("message", "글이 성공적으로 수정되었습니다!");
        }
        return "redirect:/posts";
    }

    // [기능] 게시글 삭제 처리
    // [설명] 해당 ID의 게시글을 삭제한 후 목록으로 리다이렉트한다
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.deletePost(id);
        redirectAttributes.addFlashAttribute("message", "글이 삭제되었습니다.");
        return "redirect:/posts";
    }
}