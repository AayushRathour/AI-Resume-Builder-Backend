package com.resumeai.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

/**
 * Servlet filter applied at the Gateway level.
 *
 * - Skips /api/auth/** (public login/register endpoints)
 * - Skips OPTIONS preflight requests
 * - For all other routes: validates the Bearer JWT and forwards
 * X-User-Email and X-User-Id headers to downstream services.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    // ── Paths that bypass JWT validation ─────────────────────────────────────

    private static final List<String> PUBLIC_EXACT_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/resumes/public");

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/templates",
            "/api/v1/export/file/", // file downloads are direct browser navigation — no JWT possible
            "/oauth2/",
            "/login/", // covers /login/oauth2/code/google OAuth callback
            "/actuator/");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        // Skip auth routes, oauth callback routes, and OPTIONS preflight
        return "OPTIONS".equalsIgnoreCase(method)
                || PUBLIC_EXACT_PATHS.contains(path)
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    // ── Core filter logic ─────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        
        if (path.contains("/swagger") || 
            path.contains("/v3/api-docs") || 
            path.contains("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        final Claims claims;
        try {
            claims = parseToken(token);
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        String email = extractEmail(claims);
        String userId = extractUserId(claims);
        String role = extractRole(claims);
        String plan = extractPlan(claims);

        // Wrap request to inject extra headers into the downstream call
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        if (email != null)
            mutableRequest.addHeader("X-User-Email", email);
        if (userId != null)
            mutableRequest.addHeader("X-User-Id", userId);
        if (role != null)
            mutableRequest.addHeader("X-User-Role", role);
        if (plan != null)
            mutableRequest.addHeader("X-User-Plan", plan);

        log.debug("JWT validated for email={}, plan={}", email, plan);
        filterChain.doFilter(mutableRequest, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    private String extractUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw == null) {
            String subject = claims.getSubject();
            if (subject != null && subject.matches("\\d+")) {
                return subject;
            }
        }
        return raw != null ? String.valueOf(raw) : null;
    }

    private String extractEmail(Claims claims) {
        Object raw = claims.get("email");
        if (raw != null) {
            return String.valueOf(raw);
        }
        String subject = claims.getSubject();
        return subject != null && subject.contains("@") ? subject : null;
    }

    private String extractRole(Claims claims) {
        Object raw = claims.get("role");
        return raw != null ? String.valueOf(raw) : null;
    }

    private String extractPlan(Claims claims) {
        Object raw = claims.get("subscriptionPlan");
        return raw != null ? String.valueOf(raw) : null;
    }
}
