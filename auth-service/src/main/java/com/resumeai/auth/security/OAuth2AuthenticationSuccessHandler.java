package com.resumeai.auth.security;

import com.resumeai.auth.constants.AuthConstants;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Security component supporting authentication workflows in auth-service. */
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

    /**
     * Handles OAuth2 login success and redirects with a JWT token.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            Map<String, Object> attributes = getAttributes(authentication);
            handleSuccess(response, attributes);
        } catch (Exception ex) {
            log.error("OAuth2 success handler failed", ex);
            redirectFailure(response, "OAuth handler failed");
        }
    }

    private Map<String, Object> getAttributes(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        return oauth2User.getAttributes();
    }

    private void handleSuccess(HttpServletResponse response, Map<String, Object> attrs) throws IOException {
        log.info("OAuth2 user attributes: {}", attrs);

        String email = resolveEmail(attrs);
        if (email == null || email.isBlank()) {
            redirectFailure(response, "Email not found in OAuth response");
            return;
        }

        String name = resolveName(attrs, email);
        String subject = asString(attrs.get("sub"));

        User user = processUser(email, name, subject);
        if (!isUserAllowed(response, user)) {
            return;
        }

        // Issue JWT after syncing Google profile details.
        String token = jwtUtil.generateToken(syncGoogleProfile(user, email, name));
        redirectSuccess(response, email, token);
    }

    private String resolveEmail(Map<String, Object> attrs) {
        return firstNonBlank(
                asString(attrs.get("email")),
                asString(attrs.get("preferred_username")),
                asString(attrs.get("upn"))
        );
    }

    private String resolveName(Map<String, Object> attrs, String email) {
        return firstNonBlank(
                asString(attrs.get("name")),
                asString(attrs.get("given_name")),
                email
        );
    }

    private User processUser(String email, String name, String subject) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> findLegacyGoogleUser(email, subject).orElseGet(() -> createGoogleUser(email, name)));
    }

    private Optional<User> findLegacyGoogleUser(String email, String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(subject)
                .filter(user -> user.getProvider() == Provider.GOOGLE)
                .map(user -> {
                    user.setEmail(email);
                    return userRepository.save(user);
                });
    }

    private User createGoogleUser(String email, String name) {
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
    }

    private boolean isUserAllowed(HttpServletResponse response, User user) throws IOException {
        if (user.isDeleted()) {
            redirectFailure(response, AuthConstants.ACCOUNT_DELETED);
            return false;
        }
        if (!user.isActive()) {
            redirectFailure(response, AuthConstants.ACCOUNT_SUSPENDED);
            return false;
        }
        return true;
    }

    private User syncGoogleProfile(User user, String email, String name) {
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
        if (user.getPassword() == null) {
            user.setPassword("");
            changed = true;
        }

        return changed ? userRepository.save(user) : user;
    }

    private void redirectSuccess(HttpServletResponse response, String email, String token) throws IOException {
        String redirectUrl = oauthSuccessRedirectUrl + "?token=" + urlEncode(token);
        log.info("OAuth2 success for email={} redirect={}", email, oauthSuccessRedirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void redirectFailure(HttpServletResponse response, String message) throws IOException {
        response.sendRedirect(oauthFailureRedirectUrl + "?error=" + urlEncode(message));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
