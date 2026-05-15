package com.resumeai.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final String secret = "test-secret-key-that-is-at-least-32-characters-long";

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        jwtAuthFilter = new JwtAuthFilter(restClientBuilder);
        ReflectionTestUtils.setField(jwtAuthFilter, "secret", secret);
        ReflectionTestUtils.setField(jwtAuthFilter, "authServiceBaseUrl", "http://localhost:8081");
    }

    @Test
    void shouldNotFilter_PublicPath_ReturnsTrue() {
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");

        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_OptionsMethod_ReturnsTrue() {
        when(request.getServletPath()).thenReturn("/api/v1/some-secure-path");
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_SecurePath_ReturnsFalse() {
        when(request.getServletPath()).thenReturn("/api/v1/resumes");
        when(request.getMethod()).thenReturn("GET");

        assertFalse(jwtAuthFilter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_MissingAuthHeader_Returns401() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_InvalidJwt_Returns401() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/resumes");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
    }

    @Test
    void doFilterInternal_SwaggerPath_BypassesFilter() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WebSocketPath_BypassesFilter() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/ws-notifications/123");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private String generateToken(String email, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("userId", userId);
        claims.put("role", "USER");
        claims.put("subscriptionPlan", "FREE");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
