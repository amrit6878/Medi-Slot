package com.motherson.medislot.security;

import com.motherson.medislot.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMinutes = props.jwt().expirationMinutes();
    }

    public long expiresInSeconds() { return expirationMinutes * 60; }

    public String generate(AuthUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("role", user.role().name())
                .claim("email", user.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds())));
        if (user.hospitalId() != null) builder.claim("hospitalId", user.hospitalId());
        return builder.signWith(key).compact();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Number hospitalId = claims.get("hospitalId", Number.class);
            return Optional.of(new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    Role.valueOf(claims.get("role", String.class)),
                    hospitalId == null ? null : hospitalId.longValue(),
                    claims.get("email", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
