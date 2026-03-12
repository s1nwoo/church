package com.banghwa.controller.api.admin;

import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 이미지 업로드 API
 *
 * POST /api/admin/upload/image  - 이미지 S3 업로드 → 공개 URL 반환
 * DELETE /api/admin/upload/image - 이미지 S3 삭제
 *
 * 업로드된 이미지는 S3 버킷의 uploads/cards/ 폴더에 저장됩니다.
 * CloudFront를 통해 https://bhch.kr 도메인으로 접근 가능합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RolesAllowed("ADMIN")
public class ImageUploadController {

    @Value("${aws.s3.bucket-name:bhch-frontend}")
    private String bucketName;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.cloudfront.domain:https://bhch.kr}")
    private String cloudfrontDomain;

    /**
     * 이미지 S3 업로드
     *
     * @param file 업로드할 이미지 파일 (jpg, png, gif, webp)
     * @return { "url": "https://bhch.kr/uploads/cards/xxx.png", "key": "uploads/cards/xxx.png" }
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        // 파일 유효성 검사
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일만 업로드 가능합니다."));
        }

        // 파일 크기 제한 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일 크기는 5MB 이하여야 합니다."));
        }

        try {
            // 고유 파일명 생성 (UUID + 원본 확장자)
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";
            String s3Key = "uploads/cards/" + UUID.randomUUID() + extension;

            // S3 업로드
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            s3.close();

            // CloudFront URL 반환
            String publicUrl = cloudfrontDomain + "/" + s3Key;
            log.info("이미지 업로드 완료: {}", s3Key);

            return ResponseEntity.ok(Map.of(
                    "url", publicUrl,
                    "key", s3Key
            ));

        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "업로드 중 오류가 발생했습니다."));
        }
    }

    /**
     * S3 이미지 삭제 (카드 삭제 시 연동)
     *
     * @param key S3 오브젝트 키 (예: "uploads/cards/xxx.png")
     */
    @DeleteMapping("/image")
    public ResponseEntity<Void> deleteImage(@RequestParam("key") String key) {
        // uploads/cards/ 경로만 삭제 허용 (보안)
        if (!key.startsWith("uploads/cards/")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            s3.close();

            log.info("이미지 삭제 완료: {}", key);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
