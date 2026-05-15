package com.resumeai.auth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EntityLifecycleTest {

    @Test
    void userPrePersist_shouldInitializeDefaults() {
        User user = User.builder()
                .email("user@test.com")
                .password("secret")
                .provider(Provider.LOCAL)
                .isActive(true)
                .build();

        user.prePersist();

        assertEquals(Role.USER, user.getRole());
        assertEquals(SubscriptionPlan.FREE, user.getSubscriptionPlan());
        assertNotNull(user.getCreatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    void userPreUpdate_shouldRefreshTimestamp() {
        User user = User.builder()
                .email("user@test.com")
                .password("secret")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        user.preUpdate();

        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()));
    }

    @Test
    void emailVerificationPrePersist_shouldInitializeCreatedAt() {
        EmailVerification verification = EmailVerification.builder()
                .email("user@test.com")
                .otpCode("hashed")
                .purpose(OtpPurpose.LOGIN)
                .build();

        verification.prePersist();

        assertNotNull(verification.getCreatedAt());
    }
}