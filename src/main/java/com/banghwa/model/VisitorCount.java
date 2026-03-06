// src/main/java/com/banghwa/model/VisitorCount.java
package com.banghwa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "visitor_count")
public class VisitorCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 날짜별 방문자 수 집계 (오늘 방문자 조회에 사용)
    @Column(nullable = false, unique = true)
    private LocalDate visitDate;

    // 해당 날짜의 방문자 수
    @Column(nullable = false)
    private Long dailyCount;

    // 누적 전체 방문자 수 (마지막 레코드 기준)
    @Column(nullable = false)
    private Long totalCount;
}
