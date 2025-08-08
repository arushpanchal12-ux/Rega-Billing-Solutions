package com.regabilling.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class JwtService {
    
    private final String issuer;
    private final int accessTtlMinutes;
    private final Key key;

    public JwtService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-ttl-minutes}") int accessTtlMinutes,
            @Value("${app.jwt.secret}") String secret) {
        
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
        
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, String status) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTtlMinutes * 60L);
        
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .addClaims(Map.of(
                    "email", email,
                    "status", status
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }
}
