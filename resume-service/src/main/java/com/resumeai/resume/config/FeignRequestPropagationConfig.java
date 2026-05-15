package com.resumeai.resume.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates gateway identity headers to Feign clients.
 */
@Configuration
public class FeignRequestPropagationConfig {

    /**
     * Copies auth and user context headers into downstream requests.
     */
    @Bean
    public RequestInterceptor relayRequestHeaders() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            copyHeaderIfPresent(request, template, "Authorization");
            copyHeaderIfPresent(request, template, "X-User-Id");
            copyHeaderIfPresent(request, template, "X-User-Role");
            copyHeaderIfPresent(request, template, "X-User-Plan");
        };
    }

    private void copyHeaderIfPresent(HttpServletRequest request, feign.RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}

