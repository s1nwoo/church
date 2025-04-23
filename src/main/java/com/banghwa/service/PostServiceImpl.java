package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .filter(post -> !Boolean.TRUE.equals(post.getDeleted()))
                .sorted(Comparator.comparing(Post::getCreatedDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
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
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setWriter(updatedPost.getWriter());
            post.setUpdatedDate(LocalDateTime.now());
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Override
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
