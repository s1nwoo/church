package com.banghwa.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    @TempDir Path directory;

    @Test void tokensRemainValidAfterRecreatingTheServerSigningService() throws Exception {
        Path path = directory.resolve("private/signing.key");
        var before = new JwtUtil("", path.toString());
        String token = before.generateToken("test-admin");
        var after = new JwtUtil("", path.toString());
        assertThat(after.validateToken(token)).isTrue();
        assertThat(after.getUsername(token)).isEqualTo("test-admin");
        assertThat(before.validateToken(after.generateToken("test-admin"))).isTrue();
    }

    @Test void configuredSecretIsStableAcrossDifferentServerFiles() throws Exception {
        String secret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
        var before = new JwtUtil(secret, directory.resolve("unused-a.key").toString());
        var after = new JwtUtil(secret, directory.resolve("unused-b.key").toString());
        assertThat(after.validateToken(before.generateToken("test-admin"))).isTrue();
        assertThat(Files.list(directory).toList()).isEmpty();
    }

    @Test void expiredOrTamperedTokensAreStillRejected() throws Exception {
        Path path = directory.resolve("signing.key");
        var jwt = new JwtUtil("", path.toString());
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(Files.readString(path)));
        String expired = Jwts.builder().setSubject("test-admin")
                .setExpiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        assertThat(jwt.validateToken(expired)).isFalse();
        assertThat(jwt.validateToken("invalid-token")).isFalse();
    }
}
