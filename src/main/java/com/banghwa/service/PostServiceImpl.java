// src/main/java/com/banghwa/service/PostServiceImpl.java
package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public Page<Post> searchPosts(String keyword, String category, Pageable pageable) {
        return postRepository.findAll((Specification<Post>) (root, query, cb) -> {
            // deleted=false 필터
            var predicate = cb.equal(root.get("deleted"), false);

            // 카테고리 필터
            if (category != null && !category.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            }

            // 키워드(제목 or 내용) 필터
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword + "%";
                var titleLike   = cb.like(root.get("title"),   kw);
                var contentLike = cb.like(root.get("content"), kw);
                predicate = cb.and(predicate, cb.or(titleLike, contentLike));
            }

            return predicate;
        }, pageable);
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElse(null);
    }

    @Override
    public Post savePost(Post post) {
        if (post.getCreatedDate() == null) {
            post.setCreatedDate(LocalDateTime.now());
        }
        return postRepository.save(post);
    }

    @Override
    public boolean updatePost(Long id, Post updatedPost) {
        Optional<Post> existing = postRepository.findById(id);
        if (existing.isEmpty() || Boolean.TRUE.equals(existing.get().getDeleted())) {
            return false;
        }
        Post post = existing.get();
        post.setTitle(updatedPost.getTitle());
        post.setWriter(updatedPost.getWriter());
        post.setContent(updatedPost.getContent());
        post.setUpdatedDate(LocalDateTime.now());
        postRepository.save(post);
        return true;
    }

    @Override
    public boolean deletePost(Long id) {
        Optional<Post> existing = postRepository.findById(id);
        if (existing.isEmpty() || Boolean.TRUE.equals(existing.get().getDeleted())) {
            return false;
        }
        Post post = existing.get();
        post.setDeleted(true);
        post.setDeletedDate(LocalDateTime.now());
        postRepository.save(post);
        return true;
    }
}
