// src/main/java/com/banghwa/service/PostService.java
package com.banghwa.service;

import com.banghwa.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    /**
     * 검색어(keyword)와 카테고리(category)로 필터링한 후,
     * Page 단위로 반환합니다.
     *
     * @param keyword   검색어 (제목 or 내용)
     * @param category  "공지사항" 또는 "성도소식"
     * @param pageable  페이지 번호, 크기, 정렬 정보
     */
    Page<Post> searchPosts(String keyword, String category, Pageable pageable);

    Post getPostById(Long id);
    Post savePost(Post post);
    boolean updatePost(Long id, Post updatedPost);
    boolean deletePost(Long id);
}
