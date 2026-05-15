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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

/**
 * Gateway JWT filter that validates tokens and forwards identity headers.
 * Used for centralized auth enforcement before routing to services.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${app.auth-service.base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    @Value("${app.auth-service.fallback-base-urls:http://auth-service}")
    private String authServiceFallbackBaseUrls;

    private final RestClient restClient;

    public JwtAuthFilter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // ── Paths that bypass JWT validation ─────────────────────────────────────

    private static final List<String> PUBLIC_EXACT_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/resend-otp",
            "/api/v1/resumes/public");

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/templates",
            "/api/v1/export/file/", // file downloads are direct browser navigation — no JWT possible
            "/oauth2/",
            "/login/", // covers /login/oauth2/code/google OAuth callback
            "/ws-notifications/",
            "/ws-notifications",
            "/topic/",
            "/app/",
            "/actuator/");

    /**
     * Defines public routes and preflight requests that bypass JWT validation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        boolean bypass = "OPTIONS".equalsIgnoreCase(method)
                || PUBLIC_EXACT_PATHS.contains(path)
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
        log.info("shouldNotFilter check: path={}, method={}, result={}", path, method, bypass);
        return bypass;
    }

    // ── Core filter logic ─────────────────────────────────────────────────────

    /**
     * Validates JWT, checks session via auth-service, and forwards identity headers.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("doFilterInternal path={}", path);
        
        // Fallback: also skip WebSocket/STOMP paths based on URI (getServletPath may differ)
        if (path.contains("/ws-notifications") ||
            path.startsWith("/topic/") ||
            path.startsWith("/app/")) {
            log.info("Bypassing JWT for WebSocket path={}", path);
            filterChain.doFilter(request, response);
            return;
        }

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

        // Validate session against auth-service for revoked/deleted accounts.
        if (!isSessionActive(token)) {
            sendUnauthorized(response, "Session invalid or account removed");
            return;
        }

        // Inject identity headers for downstream services.
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

    /**
     * Verifies token still maps to an active account in auth-service.
     */
    private boolean isSessionActive(String token) {
        for (String baseUrl : getAuthServiceBaseUrls()) {
            try {
                HttpStatusCode status = restClient.get()
                        .uri(baseUrl + "/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode();

                if (status.is2xxSuccessful()) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Session validation failed via auth-service baseUrl={}: {}", baseUrl, ex.getMessage());
            }
        }

        return false;
    }

    private List<String> getAuthServiceBaseUrls() {
        List<String> urls = new ArrayList<>();

        String primary = normalizeBaseUrl(authServiceBaseUrl);
        if (primary != null) {
            urls.add(primary);
        }

        if (authServiceFallbackBaseUrls != null && !authServiceFallbackBaseUrls.isBlank()) {
            for (String value : authServiceFallbackBaseUrls.split(",")) {
                String normalized = normalizeBaseUrl(value);
                if (normalized != null && !urls.contains(normalized)) {
                    urls.add(normalized);
                }
            }
        }

        return urls;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }
}
