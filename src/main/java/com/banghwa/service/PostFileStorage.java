package com.banghwa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

/** Private, persistent storage outside the JAR and all static/public resource paths. */
@Component
public class PostFileStorage {
    private final Path root;
    public PostFileStorage(@Value("${posts.attachments.directory:${user.home}/.banghwa-church/post-attachments}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    public String store(MultipartFile file) {
        String key = UUID.randomUUID().toString();
        Path destination = path(key);
        try {
            if (root.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.createDirectories(root, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                Files.createFile(destination, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } else { Files.createDirectories(root); Files.createFile(destination); }
            try (var in = file.getInputStream(); var out = Files.newOutputStream(destination, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            return key;
        } catch (IOException e) {
            try { Files.deleteIfExists(destination); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw new UncheckedIOException("첨부파일을 저장하지 못했습니다.", e);
        }
    }
    public Path path(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("첨부파일 경로가 올바르지 않습니다.");
        return root.resolve(key);
    }
    public void delete(String key) {
        try { Files.deleteIfExists(path(key)); }
        catch (IOException e) { throw new UncheckedIOException("첨부파일 삭제를 완료하지 못했습니다. 다시 시도해 주세요.", e); }
    }
}
