package com.resumeai.auth.security;

import com.resumeai.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Security utility for token generation, parsing, and validation. */
@Component
public class JwtUtil {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_SUBSCRIPTION = "subscriptionPlan";
    private static final String CLAIM_FULL_NAME = "fullName";


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generates JWT with user identity and subscription claims.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_USER_ID, user.getUserId());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_SUBSCRIPTION, user.getSubscriptionPlan().name());
        claims.put(CLAIM_FULL_NAME, user.getFullName());

        return generateToken(String.valueOf(user.getUserId()), claims);
    }

    public String generateToken(String email) {
        return generateToken(email, new HashMap<>());
    }

    private String generateToken(String email, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts email from JWT claims or subject.
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object emailClaim = claims.get(CLAIM_EMAIL);
        if (emailClaim != null) {
            return String.valueOf(emailClaim);
        }

        String subject = claims.getSubject();
        return (subject != null && subject.contains("@")) ? subject : null;
    }

    /**
     * Validates token signature and expiry time.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
