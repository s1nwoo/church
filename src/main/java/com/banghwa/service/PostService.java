package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDateTime;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // [기능] 게시글 목록 조회
    // [설명] 최신순으로 정렬된 게시글 목록을 가져와 반환한다
    public List<Post> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .filter(post -> !Boolean.TRUE.equals(post.getDeleted()))
                .sorted(Comparator.comparing(Post::getCreatedDate).reversed())
                .collect(Collectors.toList());
    }

    // [기능] 게시글 단건 조회
    // [설명] ID로 게시글을 찾아 반환한다. 없으면 null 반환
    @GetMapping("/{id}")
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    // [기능] 게시글 저장
    // [설명] 전달받은 Post 객체를 DB에 저장한다
    public Post savePost(Post post) {
        return postRepository.save(post); // 저장된 post 객체를 리턴해줌
    }

    // [기능] 게시글 수정
    // [설명] 기존 게시글을 찾아 내용을 업데이트 후 저장한다
    public boolean updatePost(Long id, Post updatedPost) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setWriter(updatedPost.getWriter());
            postRepository.save(post);
            return true;
        }
        return false;
    }

    // [기능] 게시글 삭제
    // [설명] ID에 해당하는 게시글을 삭제한다
    public boolean deletePost(Long id) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.setDeleted(true);
            post.setDeletedDate(LocalDateTime.now());
            postRepository.save(post);
            return true;
        }
        return false;
    }
}