// 📁 위치 예시: com.banghwa.config.WebConfig

package com.banghwa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 🔥 API 경로에만 적용
                .allowedOrigins("http://localhost:3000") // 🔥 리액트 개발 서버 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용 메서드 지정
                .allowCredentials(true); // (선택) 쿠키/세션 전달 허용
    }
}
