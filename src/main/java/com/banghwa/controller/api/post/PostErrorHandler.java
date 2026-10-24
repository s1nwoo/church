package com.banghwa.controller.api.post;

import com.banghwa.controller.api.admin.AdminPostController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.UncheckedIOException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {PostApiController.class, AdminPostController.class, PostAttachmentController.class})
public class PostErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, String>> storage(UncheckedIOException e) {
        return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("message", "파일당 최대 100MB, 한 번에 총 300MB까지 업로드할 수 있습니다."));
    }
}
