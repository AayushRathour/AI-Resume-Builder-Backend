package com.resumeai.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrap adminBootstrap;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(adminBootstrap, "adminEnabled", true);
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "admin@resumeai.com");
        ReflectionTestUtils.setField(adminBootstrap, "adminPassword", "adminPass");
        ReflectionTestUtils.setField(adminBootstrap, "adminFullName", "Admin");
    }

    @Test
    void ensureAdminUser_whenAdminDoesNotExist_shouldCreateAdmin() throws Exception {
        when(userRepository.findByEmail("admin@resumeai.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("adminPass")).thenReturn("encodedAdminPass");

        adminBootstrap.run();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void ensureAdminUser_whenAdminExists_shouldNotCreateAdmin() throws Exception {
        User admin = User.builder()
                .email("admin@resumeai.com")
                .role(Role.ADMIN)
                .isActive(true)
                .isDeleted(false)
                .isVerified(true)
                .password("encodedAdminPass")
                .fullName("Admin")
                .build();
        when(userRepository.findByEmail("admin@resumeai.com")).thenReturn(Optional.of(admin));

        adminBootstrap.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
