package com.banghwa.service;

import com.banghwa.model.Post;
import com.banghwa.model.PostAttachment;
import com.banghwa.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostFileStorage storage;
    public static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    @Override @Transactional(readOnly = true)
    public Page<Post> searchPosts(String keyword, String category, Pageable pageable) {
        return search(keyword, category, false, pageable);
    }
    @Override @Transactional(readOnly = true) @PreAuthorize("hasRole('ADMIN')")
    public Page<Post> searchAdminPosts(String keyword, String category, Boolean adminOnly, Pageable pageable) {
        return search(keyword, category, adminOnly, pageable);
    }
    private Page<Post> search(String keyword, String category, Boolean adminOnly, Pageable pageable) {
        return postRepository.findAll((Specification<Post>) (root, query, cb) -> {
            var predicate = cb.or(cb.isNull(root.get("deleted")), cb.isFalse(root.get("deleted")));
            if (adminOnly != null) predicate = cb.and(predicate, adminOnly
                    ? cb.isTrue(root.get("adminOnly")) : cb.or(cb.isNull(root.get("adminOnly")), cb.isFalse(root.get("adminOnly"))));
            if (category != null && !category.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword + "%";
                predicate = cb.and(predicate, cb.or(cb.like(root.get("title"), kw), cb.like(root.get("content"), kw)));
            }
            return predicate;
        }, pageable);
    }
    private boolean administrator() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    @Override @Transactional(readOnly = true)
    public Post getPostById(Long id) {
        return postRepository.findById(id).filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .filter(p -> !Boolean.TRUE.equals(p.getAdminOnly()) || administrator()).orElse(null);
    }
    private Post forUpdate(Long id) {
        return postRepository.findForUpdate(id).filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private void copy(Post source, Post target) {
        if (source.getTitle() == null || source.getTitle().isBlank() || source.getTitle().length() > 255)
            throw new IllegalArgumentException("제목은 1~255자로 입력해 주세요.");
        target.setTitle(source.getTitle().trim());
        target.setWriter(source.getWriter()); target.setCategory(source.getCategory()); target.setContent(source.getContent());
        target.setAdminOnly(Boolean.TRUE.equals(source.getAdminOnly()));
        target.setDeleted(false);
    }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public Post savePost(Post input) { return saveWithAttachments(input, List.of()); }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public Post saveWithAttachments(Post input, List<MultipartFile> files) {
        Post post = new Post(); copy(input, post);
        postRepository.save(post); addFiles(post, files);
        return postRepository.save(post);
    }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public boolean updatePost(Long id, Post input) {
        if (postRepository.findById(id).filter(p -> !Boolean.TRUE.equals(p.getDeleted())).isEmpty()) return false;
        updateWithAttachments(id, input, List.of(), List.of()); return true;
    }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public Post updateWithAttachments(Long id, Post input, List<MultipartFile> files, List<Long> removeIds) {
        Post post = forUpdate(id); copy(input, post);
        List<Long> removals = removeIds == null ? List.of() : removeIds;
        if (!post.getAttachments().stream().map(PostAttachment::getId).toList().containsAll(removals))
            throw new IllegalArgumentException("이 게시글의 첨부파일만 삭제할 수 있습니다.");
        if (post.getAttachments().size() - removals.stream().distinct().count() + (files == null ? 0 : files.size()) > 10)
            throw new IllegalArgumentException("첨부파일은 게시글당 최대 10개입니다.");
        addFiles(post, files);
        for (var attachment : List.copyOf(post.getAttachments())) {
            if (removals.contains(attachment.getId())) { storage.delete(attachment.getStorageKey()); post.getAttachments().remove(attachment); }
        }
        post.setUpdatedDate(LocalDateTime.now());
        return postRepository.save(post);
    }
    private void addFiles(Post post, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;
        if (files.size() > 10) throw new IllegalArgumentException("첨부파일은 게시글당 최대 10개입니다.");
        if (files.stream().mapToLong(MultipartFile::getSize).sum() > 300L * 1024 * 1024)
            throw new IllegalArgumentException("한 번에 총 300MB까지 업로드할 수 있습니다.");
        for (var file : files) {
            if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("각 첨부파일은 0바이트 초과, 100MB 이하여야 합니다.");
        }
        for (var file : files) {
            String key = storage.store(file);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) storage.delete(key); }
            });
            var attachment = new PostAttachment();
            String original = file.getOriginalFilename() == null ? "첨부파일" : file.getOriginalFilename().replace('\\', '/');
            original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
            attachment.setFilename(original.isBlank() ? "첨부파일" : original.substring(0, Math.min(200, original.length())));
            attachment.setSize(file.getSize()); attachment.setStorageKey(key); attachment.setPost(post);
            post.getAttachments().add(attachment);
        }
    }
    @Override @Transactional(readOnly = true)
    public PostAttachment getAttachment(Long postId, Long attachmentId) {
        Post post = getPostById(postId);
        if (post == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return post.getAttachments().stream().filter(a -> a.getId().equals(attachmentId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public boolean deletePost(Long id) {
        var found = postRepository.findForUpdate(id);
        if (found.isEmpty() || Boolean.TRUE.equals(found.get().getDeleted())) return false;
        Post post = found.get();
        // A failed filesystem deletion aborts the request. Keep metadata so deletion can be retried.
        for (var attachment : post.getAttachments()) storage.delete(attachment.getStorageKey());
        post.getAttachments().clear();
        if (Boolean.TRUE.equals(post.getAdminOnly())) postRepository.delete(post);
        else { post.setDeleted(true); post.setDeletedDate(LocalDateTime.now()); postRepository.save(post); }
        return true;
    }
}
