package com.resumeai.auth.config;

import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the default admin account for secure first-time service access.
 * Runs at startup to ensure governance endpoints remain reachable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${app.admin.email:admin@resumeai.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.full-name:ResumeAI Admin}")
    private String adminFullName;

    /**
     * Ensures an admin user exists and is active for system access.
     */
    @Override
    public void run(String... args) {
        if (!adminEnabled) {
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("[AUTH] Admin bootstrap skipped: missing admin email or password.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            User admin = User.builder()
                    .fullName(adminFullName == null || adminFullName.isBlank() ? "Admin" : adminFullName)
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .provider(Provider.LOCAL)
                    .isActive(true)
                    .isVerified(true)
                    .isDeleted(false)
                    .subscriptionPlan(SubscriptionPlan.FREE)
                    .build();
            userRepository.save(admin);
            log.info("[AUTH] Default admin account created: email={}", normalizedEmail);
            return;
        }

        boolean changed = false;
        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            changed = true;
        }
        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }
        if (user.isDeleted()) {
            user.setDeleted(false);
            user.setDeletedAt(null);
            changed = true;
        }
        if (!user.isVerified()) {
            user.setVerified(true);
            changed = true;
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(adminFullName == null || adminFullName.isBlank() ? "Admin" : adminFullName);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
            log.info("[AUTH] Default admin account updated: email={}", normalizedEmail);
        }
    }
}
