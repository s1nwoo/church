// src/main/java/com/banghwa/controller/api/post/PostApiController.java
package com.banghwa.controller.api.post;

import com.banghwa.model.Post;
import com.banghwa.service.PostService;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    /**
     * [기능] 검색 + 카테고리 + 페이징
     * - keyword: 제목 또는 내용 키워드
     * - category: "공지사항" 또는 "성도소식"
     * - page: 0부터 시작하는 페이지 번호
     * - size: 한 페이지에 보여줄 개수
     */
    @GetMapping
    public Page<Post> getPosts(
            @RequestParam(defaultValue = "")         String keyword,
            @RequestParam(defaultValue = "공지사항") String category,
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "10")       int size
    ) {
        return postService.searchPosts(
                keyword,
                category,
                PageRequest.of(page, size, Sort.by("createdDate").descending())
        );
    }

    /** [기능] 게시글 등록 */
    @PostMapping
    @RolesAllowed("ADMIN")
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post saved = postService.savePost(post);
        return ResponseEntity.ok(saved);
    }

    /** [기능] 게시글 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable("id") Long id) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    /** [기능] 게시글 수정 */
    @PutMapping("/{id}")
    @RolesAllowed("ADMIN")
    public ResponseEntity<Post> updatePost(
            @PathVariable("id") Long id,
            @RequestBody Post updatedPost
    ) {
        boolean ok = postService.updatePost(id, updatedPost);
        return ok
                ? ResponseEntity.ok(postService.getPostById(id))
                : ResponseEntity.notFound().build();
    }

    /** [기능] 게시글 삭제 (soft-delete) */
    @DeleteMapping("/{id}")
    @RolesAllowed("ADMIN")
    public ResponseEntity<Void> deletePost(@PathVariable("id") Long id) {
        boolean deleted = postService.deletePost(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
