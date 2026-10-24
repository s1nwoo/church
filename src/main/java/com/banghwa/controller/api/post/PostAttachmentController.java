package com.banghwa.controller.api.post;

import com.banghwa.service.PostFileStorage;
import com.banghwa.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RestController @RequestMapping("/api/posts/{postId}/attachments") @RequiredArgsConstructor
public class PostAttachmentController {
    private final PostService service;
    private final PostFileStorage storage;
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long postId, @PathVariable Long id) {
        var attachment = service.getAttachment(postId, id);
        var path = storage.path(attachment.getStorageKey());
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(attachment.getSize())
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }
}
