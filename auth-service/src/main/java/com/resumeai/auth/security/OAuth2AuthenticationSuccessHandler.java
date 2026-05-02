package com.resumeai.auth.security;

import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.success-redirect-url:http://localhost:3000/oauth/callback}")
    private String oauthSuccessRedirectUrl;

    @Value("${app.oauth2.failure-redirect-url:http://localhost:3000/login}")
    private String oauthFailureRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attrs = oauth2User.getAttributes();
            log.info("OAuth2 user attributes: {}", attrs);

            String email = firstNonBlank(
                    asString(attrs.get("email")),
                    asString(attrs.get("preferred_username")),
                    asString(attrs.get("upn"))
            );

            String name = firstNonBlank(
                    asString(attrs.get("name")),
                    asString(attrs.get("given_name")),
                    email
            );

            String subject = asString(attrs.get("sub"));

            if (email == null || email.isBlank()) {
                redirectFailure(response, "Email not found in Google OAuth response");
                return;
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // Self-heal legacy rows created with sub saved as email.
                        if (subject != null && !subject.isBlank()) {
                            User legacy = userRepository.findByEmail(subject).orElse(null);
                            if (legacy != null && legacy.getProvider() == Provider.GOOGLE) {
                                legacy.setEmail(email);
                                legacy.setFullName((name == null || name.isBlank()) ? email : name);
                                return userRepository.save(legacy);
                            }
                        }

                        return userRepository.save(User.builder()
                                .fullName((name == null || name.isBlank()) ? email : name)
                                .email(email)
                                .password("")
                                .phone(null)
                                .role(Role.USER)
                                .provider(Provider.GOOGLE)
                                .isActive(true)
                                .subscriptionPlan(SubscriptionPlan.FREE)
                                .build());
                    });

            // Keep Google users synced with latest profile info from provider.
            boolean changed = false;
            if (user.getProvider() != Provider.GOOGLE) {
                user.setProvider(Provider.GOOGLE);
                changed = true;
            }
            if (name != null && !name.isBlank() && !name.equals(user.getFullName())) {
                user.setFullName(name);
                changed = true;
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                changed = true;
            }
            if (changed) {
                user = userRepository.save(user);
            }

            if (user.getPassword() == null) {
                user.setPassword("");
                user = userRepository.save(user);
            }

            if (!user.isActive()) {
                redirectFailure(response, "Account is deactivated");
                return;
            }

            String token = jwtUtil.generateToken(user);
            String redirectUrl = oauthSuccessRedirectUrl + "?token=" + urlEncode(token);
            log.info("OAuth2 success for email={} redirect={}", email, oauthSuccessRedirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            log.error("OAuth2 success handler failed", ex);
            redirectFailure(response, "OAuth handler failed");
        }
    }

    private void redirectFailure(HttpServletResponse response, String message) throws IOException {
        response.sendRedirect(oauthFailureRedirectUrl + "?error=" + urlEncode(message));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
