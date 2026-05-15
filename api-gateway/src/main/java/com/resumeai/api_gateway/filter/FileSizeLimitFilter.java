package com.resumeai.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gateway filter that blocks oversized multipart uploads early.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FileSizeLimitFilter extends OncePerRequestFilter {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    /**
     * Rejects multipart requests larger than the configured limit.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE) {
                response.setStatus(413); // 413 Payload Too Large
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Payload Too Large\",\"message\":\"File size exceeds the 5MB limit. Please upload a smaller file.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
