package com.resumeai.auth.service.impl;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.InvalidCredentialsException;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import com.resumeai.auth.service.AuthService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request == null
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidCredentialsException("Email and password are required");
        }

        String email = request.getEmail().trim();
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            if (!existingUser.isActive()) {
                throw new RuntimeException("Account is deactivated");
            }

            if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()) {
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                if (request.getFullName() != null && !request.getFullName().isBlank()) {
                    existingUser.setFullName(request.getFullName());
                }
                existingUser = userRepository.save(existingUser);
            }

            if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }

            String token = jwtUtil.generateToken(existingUser);
            return AuthResponse.builder()
                    .token(token)
                    .userId(existingUser.getUserId())
                    .email(existingUser.getEmail())
                    .fullName(existingUser.getFullName())
                    .role(existingUser.getRole().name())
                    .subscriptionPlan(existingUser.getSubscriptionPlan().name())
                    .build();
        }

        User user = User.builder()
                .fullName((request.getFullName() == null || request.getFullName().isBlank()) ? email : request.getFullName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .isActive(true)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .subscriptionPlan(savedUser.getSubscriptionPlan().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .subscriptionPlan(user.getSubscriptionPlan().name())
                .build();
    }

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        return toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(String email, RegisterRequest request) {
        User user = getActiveUserByEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        User updatedUser = userRepository.save(user);
        return toProfileResponse(updatedUser);
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = getActiveUserByEmail(email);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(String email) {
        User user = getActiveUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public UserProfileResponse updateSubscription(String email, String plan) {
        User user = getActiveUserByEmail(email);
        return updateSubscriptionForUser(user, plan);
    }

    @Override
    public UserProfileResponse updateSubscriptionByUserId(Long userId, String plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return updateSubscriptionForUser(user, plan);
    }

    @Override
    public UserProfileResponse upgradeUserToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(Role.ADMIN);
        user.setActive(true);
        User updatedUser = userRepository.save(user);
        return toProfileResponse(updatedUser);
    }

    @Override
    public UserProfileResponse suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        User updatedUser = userRepository.save(user);
        return toProfileResponse(updatedUser);
    }

    private UserProfileResponse updateSubscriptionForUser(User user, String plan) {
        SubscriptionPlan subscriptionPlan;
        try {
            subscriptionPlan = SubscriptionPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid subscription plan. Use FREE or PREMIUM");
        }

        user.setSubscriptionPlan(subscriptionPlan);
        User updatedUser = userRepository.save(user);
        return toProfileResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toProfileResponse)
                .toList();
    }

    @Override
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }

    private User getActiveUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        return user;
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .provider(user.getProvider().name())
                .isActive(user.isActive())
                .subscriptionPlan(user.getSubscriptionPlan().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
