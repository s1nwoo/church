package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.data.domain.Sort;
@Service // 이 클래스는 비즈니스 로직을 담당하는 "서비스 계층"임을 명시
@RequiredArgsConstructor // 생성자 자동 생성 (final 필드 주입용)
public class CommunityService {

    // DB에 접근할 수 있도록 PostRepository 주입
    private final PostRepository postRepository;

    // Post 목록 조회
    public List<Post> getAllPosts() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")); // 최신순 정렬
    }

    // 게시글 저장 메서드
    public void savePost(Post post) {
        postRepository.save(post); // 이 한 줄로 DB에 저장됨!
    }
}
