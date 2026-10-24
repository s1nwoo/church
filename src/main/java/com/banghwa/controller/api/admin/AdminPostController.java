package com.banghwa.controller.api.admin;

import com.banghwa.model.Post;
import com.banghwa.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class AdminPostController {
    private final PostService service;
    @GetMapping
    public ResponseEntity<Page<Post>> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category, @RequestParam(required = false) Boolean adminOnly,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        var posts = service.searchAdminPosts(keyword, category, adminOnly,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), Sort.by("createdDate").descending()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(posts);
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Post create(@RequestPart("post") Post post, @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return service.saveWithAttachments(post, files);
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Post update(@PathVariable Long id, @RequestPart("post") Post post,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "removeAttachmentIds", required = false) List<Long> removeIds) {
        return service.updateWithAttachments(id, post, files, removeIds);
    }
}
