package com.resumeai.auth.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Configures Feign behavior for inter-service communication. */
@Configuration
public class FeignRequestPropagationConfig {

    /**
     * Copies identity headers from the current HTTP request to Feign requests.
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


