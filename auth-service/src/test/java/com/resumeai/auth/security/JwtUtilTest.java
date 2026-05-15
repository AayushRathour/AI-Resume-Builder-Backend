package com.resumeai.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "resumeai_super_secret_key_change_me_32_chars_minimum");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L);
    }

    @Test
    void generateToken_fromEmail_shouldExtractEmailAndValidate() {
        String token = jwtUtil.generateToken("user@test.com");

        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals("user@test.com", extractedEmail);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void generateToken_fromUser_shouldExtractEmailAndValidate() {
        User user = User.builder()
                .userId(1L)
                .email("user1@test.com")
                .fullName("Test User")
                .role(Role.USER)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build();

        String token = jwtUtil.generateToken(user);

        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals("user1@test.com", extractedEmail);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("this-is-not-a-valid-jwt"));
    }
}
