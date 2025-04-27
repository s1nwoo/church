// 📁 위치: com.banghwa.config.WebConfig.java

package com.banghwa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 🔥 API 경로에만 CORS 적용
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://church-frontend.vercel.app" // 🔥 네 Vercel 배포 URL 추가
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 🔥 OPTIONS 추가 (Preflight 요청 대비)
                .allowCredentials(true);
    }
}
