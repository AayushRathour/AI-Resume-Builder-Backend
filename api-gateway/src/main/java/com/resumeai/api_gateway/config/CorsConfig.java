package com.resumeai.api_gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * GLOBAL CORS — applied ONLY at the Gateway.
 *
 * All downstream services (resume, template, auth, etc.) must NOT add their
 * own CORS headers.  Duplicate headers cause:
 *   "The 'Access-Control-Allow-Origin' header contains multiple values"
 *
 * CRITICAL: Wrapped in FilterRegistrationBean with HIGHEST_PRECEDENCE so that
 * CORS headers are always present on EVERY response — including error responses
 * produced by JwtAuthFilter (401/403).  Without this, the browser sees a
 * response without Access-Control-Allow-Origin and reports "Network Error"
 * instead of the actual HTTP status.
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:5173"); // Vite dev server
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Content-Disposition"); // needed for file downloads
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply gateway-level CORS to REST APIs only.
        // WebSocket SockJS endpoint (/ws-notifications/**) is CORS-handled by notification-service
        // to avoid duplicate Access-Control-Allow-Origin headers.
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // MUST run before JwtAuthFilter
        return bean;
    }
}
