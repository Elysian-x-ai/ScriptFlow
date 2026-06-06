package com.scriptflow.framework.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token utility for authentication.
 * Generates and validates HMAC-SHA256 signed tokens.
 */
@Slf4j
public class JwtUtil {

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    private static SecretKey secretKey;
    private static long expirationMs;

    private JwtUtil() {}

    public static void init(String secret, long expirationMs) {
        JwtUtil.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JwtUtil.expirationMs = expirationMs;
    }

    public static String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public static boolean validateToken(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /** Store current userId for the request (called by JwtAuthFilter). */
    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    /** Get current userId for the request. */
    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    /** Clear current userId (called by JwtAuthFilter after request completes). */
    public static void removeCurrentUserId() {
        currentUserId.remove();
    }
}
