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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner ensureAdminUser(
            @Value("${app.admin.email:admin@resumeai.com}") String adminEmail,
            @Value("${app.admin.password:Admin@12345}") String adminPassword,
            @Value("${app.admin.name:Admin}") String adminName) {
        return args -> {
            userRepository.findByEmail(adminEmail)
                    .ifPresentOrElse(existing -> {
                        if (existing.getRole() != Role.ADMIN || !existing.isActive()) {
                            existing.setRole(Role.ADMIN);
                            existing.setActive(true);
                            userRepository.save(existing);
                            log.info("Promoted existing user to ADMIN: {}", adminEmail);
                        }
                    }, () -> {
                        User admin = User.builder()
                                .fullName(adminName)
                                .email(adminEmail)
                                .password(passwordEncoder.encode(adminPassword))
                                .role(Role.ADMIN)
                                .provider(Provider.LOCAL)
                                .isActive(true)
                                .subscriptionPlan(SubscriptionPlan.PREMIUM)
                                .build();
                        userRepository.save(admin);
                        log.info("Created default ADMIN user: {}", adminEmail);
                    });
        };
    }
}
