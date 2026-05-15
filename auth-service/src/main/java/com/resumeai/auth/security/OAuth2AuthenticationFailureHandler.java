package com.resumeai.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** Security component supporting authentication workflows in auth-service. */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-url:http://localhost:3000/login}")
    private String oauthFailureRedirectUrl;

    /**
     * Handles OAuth2 failures and performs a safe redirect.
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("OAuth2 authentication failed. uri={} message={}",
            request.getRequestURI(),
            exception != null ? exception.getMessage() : "unknown",
            exception);

        String message = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "OAuth authentication failed";

        String redirectUrl = oauthFailureRedirectUrl + "?error=" + urlEncode(message);
        response.sendRedirect(redirectUrl);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
