package com.banghwa.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.Key;
import java.util.Date;
import java.util.Set;

@Component
public class JwtUtil {

    private final Key key;
    private final long validityInMs = 1000 * 60 * 60; // 1시간

    public JwtUtil(@Value("${jwt.secret:}") String configuredSecret,
                   @Value("${jwt.secret-file:${user.home}/.banghwa-church/jwt-signing.key}") String secretFile) throws IOException {
        // An explicit base64 secret supports multi-instance deployments. A private file
        // keeps the same key across restarts on the existing single EC2 instance.
        String secret = configuredSecret.isBlank() ? loadOrCreateSecret(Path.of(secretFile)) : configuredSecret.trim();
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private static String loadOrCreateSecret(Path file) throws IOException {
        Path path = file.toAbsolutePath().normalize();
        boolean posix = path.getFileSystem().supportedFileAttributeViews().contains("posix");
        if (posix) Files.createDirectories(path.getParent(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        else Files.createDirectories(path.getParent());
        var options = Set.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try (FileChannel channel = posix
                ? FileChannel.open(path, options, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
                : FileChannel.open(path, options);
             var ignored = channel.lock()) {
            if (channel.size() == 0) {
                String secret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
                ByteBuffer bytes = StandardCharsets.US_ASCII.encode(secret);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
                return secret;
            }
            if (channel.size() > 4096) throw new IOException("JWT signing key file is invalid");
            ByteBuffer bytes = ByteBuffer.allocate((int) channel.size());
            while (bytes.hasRemaining() && channel.read(bytes) != -1) { /* Read under the file lock. */ }
            bytes.flip();
            return StandardCharsets.US_ASCII.decode(bytes).toString().trim();
        }
    }

    /** [기능] 토큰 생성
     * [설명] 사용자 이름과 만료 시간을 담아 JWT 발급 */
    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityInMs))
                .signWith(key)
                .compact();
    }

    /** [기능] 토큰에서 사용자명 추출 */
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** [기능] 토큰 유효성 검사 */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
