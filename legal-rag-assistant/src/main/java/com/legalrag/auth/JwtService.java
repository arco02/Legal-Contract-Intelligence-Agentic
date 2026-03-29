package com.legalrag.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT creation and validation using JJWT 0.12.x.
 *
 * Token anatomy:
 *   Header  : { "alg": "HS256" }
 *   Payload : { "sub": "user@email.com", "iat": <issued>, "exp": <expiry> }
 *   Signature: HMAC-SHA256(header + payload, secret)
 *
 * The subject (sub) claim stores the user's email, which is also the
 * Spring Security principal username. This lets JwtAuthFilter resolve
 * the user from the token without a database call.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryMs;

    /**
     * Build the HMAC-SHA256 signing key from the application secret.
     *
     * The secret must be at least 32 characters (256 bits) to satisfy
     * HS256 requirements — JJWT will throw WeakKeyException on startup
     * if it is too short, which is intentional: fail fast rather than
     * silently produce insecure tokens.
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiry-ms}") long expiryMs) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
    }

    /**
     * Generate a signed JWT for the given email (used as the subject).
     * Called by AuthService after successful login or registration.
     *
     * @param email the authenticated user's email address
     * @return compact, URL-safe JWT string
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the subject (email) from a token.
     * Throws JwtException if the token is invalid or expired —
     * callers should catch this and return 401.
     *
     * @param token raw JWT string (without "Bearer " prefix)
     * @return the email stored in the subject claim
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Full validation: signature check + expiry check.
     * Returns true only if both pass and the subject matches the given email.
     *
     * Used by JwtAuthFilter on every authenticated request.
     *
     * @param token raw JWT string
     * @param email expected subject to match against
     * @return true if token is valid and belongs to this email
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String subject = extractEmail(token);
            return subject.equals(email) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        // JJWT 0.12.x — verifyWith() replaces the old setSigningKey()
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }
}