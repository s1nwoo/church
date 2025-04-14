package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.repository.PostRepository;
import com.banghwa.ChurchApplication; // 👈 너의 main class import
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = ChurchApplication.class) // 👈 명시적으로 루트 설정 지정
public class CommunityServiceTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    public void 게시글_저장_테스트() {
        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .writer("홍길동")
                .content("테스트 내용입니다.")
                .build();

        // when
        postRepository.save(post);

        // then
        List<Post> postList = postRepository.findAll();
        Assertions.assertThat(postList).hasSize(1);
        Assertions.assertThat(postList.get(0).getTitle()).isEqualTo("테스트 제목");
    }
}
