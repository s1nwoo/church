package com.banghwa.service;

import com.banghwa.model.Post;
import java.util.List;

public interface PostService {
    List<Post> getAllPosts();
    Post getPostById(Long id);
    Post savePost(Post post);
    boolean updatePost(Long id, Post updatedPost);
    boolean deletePost(Long id);
}
