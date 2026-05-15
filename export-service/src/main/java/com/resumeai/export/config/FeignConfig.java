package com.resumeai.export.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Configures Feign behavior for inter-service communication. */

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            var request = attrs.getRequest();
            String auth = request.getHeader("Authorization");
            String userId = request.getHeader("X-User-Id");
            if (auth != null && !auth.isBlank()) {
                template.header("Authorization", auth);
            }
            if (userId != null && !userId.isBlank()) {
                template.header("X-User-Id", userId);
            }
        };
    }
}

