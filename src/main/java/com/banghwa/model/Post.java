package com.banghwa.model; // 너의 프로젝트 패키지에 맞게 조정해줘!

import jakarta.persistence.*;  // JPA 어노테이션 사용
import lombok.*;              // Lombok으로 코드 간결하게
import java.time.LocalDateTime;

@Entity // 이 클래스가 DB 테이블과 매핑된다는 의미
@Getter @Setter // Lombok: getter, setter 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 받는 생성자 자동 생성
@Builder // 객체를 쉽게 만들 수 있는 builder 패턴 제공
public class Post {

    @Id // 기본 키(primary key) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 자동 증가 전략 (MySQL의 AUTO_INCREMENT 같은 느낌)
    private Long id;

    private String title;  // 게시글 제목

    private String writer; // 작성자

    @Column(columnDefinition = "TEXT") // 긴 글도 저장 가능하도록 TEXT 타입 설정
    private String content; // 게시글 내용

    private LocalDateTime createdDate; // 글 작성 시간

    @PrePersist // DB에 저장되기 **직전**에 실행되는 메서드
    public void prePersist() {
        this.createdDate = LocalDateTime.now(); // 작성 시간 자동 저장
    }
}
