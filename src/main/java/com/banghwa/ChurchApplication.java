package com.banghwa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ← 이 줄 추가
public class ChurchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChurchApplication.class, args);
    }
}
