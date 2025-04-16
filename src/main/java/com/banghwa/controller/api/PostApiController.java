package com.banghwa.controller.api;

import com.banghwa.model.Post;
import com.banghwa.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    // [기능] 모든 게시글 조회
    @GetMapping
    public List<Post> getPosts() {
        return postService.getAllPosts(); // 🔁 서비스 계층 사용
    }

    // [기능] 게시글 등록
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post saved = postService.savePost(post);
        return ResponseEntity.ok(saved);
    }

    // [기능] 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable("id") Long id) {
        Post post = postService.getPostById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    // [기능] 게시글 삭제 (소프트 딜리트)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable("id") Long id) {
        boolean deleted = postService.deletePost(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<Post> updatePost(@PathVariable("id") Long id, @RequestBody Post updatedPost) {
        Post post = postService.getPostById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return ResponseEntity.notFound().build();
        }

        post.setTitle(updatedPost.getTitle());
        post.setWriter(updatedPost.getWriter());
        post.setContent(updatedPost.getContent());
        post.setUpdatedDate(LocalDateTime.now());

        Post saved = postService.savePost(post);
        return ResponseEntity.ok(saved);
    }


}
