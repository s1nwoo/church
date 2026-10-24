package com.banghwa.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.*;
import org.springframework.util.unit.DataSize;

@Configuration
public class PostAttachmentConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        var config = new MultipartConfigFactory();
        config.setMaxFileSize(DataSize.ofMegabytes(100));
        config.setMaxRequestSize(DataSize.ofMegabytes(310)); // 300MB files plus multipart headers/JSON.
        return config.createMultipartConfig();
    }
}
