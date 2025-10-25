package com.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            // Проверяем что токен не пустой
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token is null or empty");
                return false;
            }

            // Проверяем базовую структуру JWT (должен содержать 3 части разделенные точками)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT structure. Expected 3 parts, got: {}", parts.length);
                return false;
            }

            // Извлекаем claims и проверяем обязательные поля
            Claims claims = extractClaims(token);

            // Проверяем subject (username)
            String username = claims.getSubject();
            if (username == null || username.trim().isEmpty()) {
                log.warn("Token missing subject (username)");
                return false;
            }

            // Проверяем expiration date
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                log.warn("Token missing expiration date");
                return false;
            }

            if (expiration.before(new Date())) {
                log.warn("Token expired at: {}", expiration);
                return false;
            }

            // Проверяем issued at date (опционально, но рекомендуется)
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null && issuedAt.after(new Date())) {
                log.warn("Token issued in the future: {}", issuedAt);
                return false;
            }

            log.debug("Token validation successful for user: {}", username);
            return true;

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid token argument: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected token validation error: {}", e.getMessage());
            return false;
        }
    }
}