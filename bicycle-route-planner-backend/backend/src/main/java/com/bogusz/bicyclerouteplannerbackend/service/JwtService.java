package com.bogusz.bicyclerouteplannerbackend.service;

import com.bogusz.bicyclerouteplannerbackend.config.JwtConfig;
import com.bogusz.bicyclerouteplannerbackend.exception.RefreshTokenException;
import com.bogusz.bicyclerouteplannerbackend.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        String secret = jwtConfig.getSecret();
        byte[] keyBytes = (secret != null) ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser().verifyWith(this.signingKey).build();
    }

    public String generateAccessToken(UserPrincipal principal) {
        return generateToken(
                Map.of(
                        "type", "ACCESS",
                        "userId", principal.getId(),
                        "role", principal.getRole()
                ),
                principal.getEmail(),
                jwtConfig.getAccessTokenExpirationMs()
        );
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return generateToken(
                Map.of(
                        "type", "REFRESH",
                        "userId", principal.getId()
                ),
                principal.getEmail(),
                jwtConfig.getRefreshTokenExpirationMs()
        );
    }

    public String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<UserPrincipal> parseAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (!"ACCESS".equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            Long userId = claims.get("userId", Long.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || email == null || email.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UserPrincipal.fromJwt(userId, email, role != null ? role : "ROLE_USER"));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String extractEmailFromRefreshToken(String refreshToken) {
        try {
            Claims claims = extractAllClaims(refreshToken);
            if (!"REFRESH".equals(claims.get("type", String.class))) {
                throw new RefreshTokenException("Invalid token type");
            }
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new RefreshTokenException("Invalid refresh token");
            }
            return subject;
        } catch (JwtException | IllegalArgumentException e) {
            throw new RefreshTokenException("Invalid refresh token", e);
        }
    }

    public long getAccessTokenExpirationMs() {
        return jwtConfig.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtConfig.getRefreshTokenExpirationMs();
    }

    Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }
}
