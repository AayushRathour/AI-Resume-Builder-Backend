package com.resumeai.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.InvalidCredentialsException;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .fullName("Test User")
                .email("user@test.com")
                .password("plain-password")
                .phone("9999999999")
                .build();
    }

    @Test
    void register_shouldSaveDefaultUserAndReturnToken() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("USER", response.getRole());
        assertEquals("FREE", response.getSubscriptionPlan());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(Provider.LOCAL, savedUser.getProvider());
        assertEquals(SubscriptionPlan.FREE, savedUser.getSubscriptionPlan());
        assertTrue(savedUser.isActive());
        assertEquals("hashed-password", savedUser.getPassword());
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));

        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@test.com")
                .password("plain-password")
                .build();

        User user = User.builder()
                .userId(2L)
                .fullName("Test User")
                .email("user@test.com")
                .password("hashed-password")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(2L, response.getUserId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("USER", response.getRole());
        assertEquals("FREE", response.getSubscriptionPlan());
    }

    @Test
    void login_shouldThrowForInvalidPassword() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@test.com")
                .password("wrong-password")
                .build();

        User user = User.builder()
                .email("user@test.com")
                .password("hashed-password")
                .role(Role.USER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void changePassword_shouldUpdatePasswordWhenOldPasswordMatches() {
        User user = User.builder()
                .email("user@test.com")
                .password("old-hashed")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hashed")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hashed");

        authService.changePassword("user@test.com", "old-password", "new-password");

        assertEquals("new-hashed", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updateSubscription_shouldThrowForInvalidPlan() {
        User user = User.builder()
                .email("user@test.com")
                .password("hashed")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateSubscription("user@test.com", "gold"));

        assertEquals("Invalid subscription plan. Use FREE or PREMIUM", ex.getMessage());
    }
}
