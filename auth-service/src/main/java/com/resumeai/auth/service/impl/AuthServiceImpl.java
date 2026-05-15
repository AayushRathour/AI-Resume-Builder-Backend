package com.resumeai.auth.service.impl;

import com.resumeai.auth.constants.AuthMessages;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.OtpResponse;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.entity.OtpPurpose;
import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.AccountDeletedException;
import com.resumeai.auth.exception.AccountSuspendedException;
import com.resumeai.auth.exception.InvalidCredentialsException;
import com.resumeai.auth.exception.InvalidOtpException;
import com.resumeai.auth.exception.UserNotFoundException;
import com.resumeai.auth.repository.EmailVerificationRepository;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.service.OtpService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements authentication workflows and service-layer orchestration. */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    /**
     * Creates a user or resumes login flow, then issues OTP for verification.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            return handleExistingUserRegistration(existingUser, request, email);
        }

        User savedUser = createNewUser(request, email);
        return generateRegisterOtpResponse(savedUser, email, request.getFullName());
    }

    private AuthResponse handleExistingUserRegistration(User existingUser, RegisterRequest request, String email) {
        validateUserStatus(existingUser);
        setupPasswordIfBlank(existingUser, request);
        validatePassword(request.getPassword(), existingUser.getPassword());
        return generateLoginOtpResponse(existingUser, email);
    }

    /**
     * Validates credentials and returns JWT or OTP requirement based on verification state.
     */
    @Override
    @Transactional(readOnly = false)
    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);
        String email = request.getEmail().trim().toLowerCase();
        return authenticateAndGenerateOtp(request, email);
    }

    private AuthResponse authenticateAndGenerateOtp(LoginRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(AuthMessages.INVALID_CREDENTIALS));

        validateUserStatus(user);
        validatePassword(request.getPassword(), user.getPassword());

        // Users who already completed email verification should not be asked for OTP again.
        if (user.isVerified()) {
            String token = jwtUtil.generateToken(user);
            return buildAuthResponse(user, token);
        }

        // OTP challenge for unverified accounts.
        return generateLoginOtpResponse(user, email);
    }

    /**
     * Generates OTP with rate limiting and cooldown enforcement.
     */
    @Override
    public OtpResponse sendOtp(String email, String purposeStr) {
        email = email.trim().toLowerCase();
        OtpPurpose purpose = parseOtpPurpose(purposeStr);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if (user.isDeleted()) {
                return buildFailedOtpResponse(email, AuthMessages.ACCOUNT_DELETED_CONTACT_ADMIN);
            }
            if (!user.isActive()) {
                return buildFailedOtpResponse(email, AuthMessages.ACCOUNT_SUSPENDED_CONTACT_SUPPORT);
            }
        }

        Long userId = user != null ? user.getUserId() : null;
        String name = user != null && user.getFullName() != null ? user.getFullName() : "User";

        OtpService.OtpGenerationResult result = otpService.generateOtp(email, purpose, userId);
        return handleOtpGenerationResult(result, email, name);
    }

    /**
     * Verifies OTP, marks user verified, and issues JWT on success.
     */
    @Override
    public OtpResponse verifyOtp(String email, String otp, String purposeStr) {
        email = email.trim().toLowerCase();
        OtpPurpose purpose = parseOtpPurpose(purposeStr);

        OtpService.OtpVerificationResult result = otpService.verifyOtp(email, otp, purpose);

        if (!result.isSuccess()) {
            return buildFailedOtpResponse(email, result.getMessage());
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));

        validateUserStatus(user);
        markUserAsVerified(user, email);

        String token = jwtUtil.generateToken(user);
        AuthResponse authResponse = buildAuthResponse(user, token);

        log.info("[AUTH] JWT issued after OTP verification for email={}", email);

        return OtpResponse.builder()
                .success(true)
                .message(AuthMessages.EMAIL_VERIFIED_SUCCESSFULLY)
                .email(email)
                .authResponse(authResponse)
                .build();
    }

    @Override
    public OtpResponse resendOtp(String email, String purposeStr) {
        return sendOtp(email, purposeStr);
    }

    @Override
    public UserProfileResponse getProfile(String email) {
        return toProfileResponse(getActiveUserByEmail(email));
    }

    @Override
    public UserProfileResponse updateProfile(String email, RegisterRequest request) {
        User user = getActiveUserByEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        return toProfileResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = getActiveUserByEmail(email);
        validatePassword(oldPassword, user.getPassword());
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(String email) {
        User user = getActiveUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
        invalidateOtpForUser(user);
    }

    /**
     * Updates subscription plan for the authenticated user.
     */
    @Override
    public UserProfileResponse updateSubscription(String email, String plan) {
        return updateSubscriptionForUser(getActiveUserByEmail(email), plan);
    }

    @Override
    public UserProfileResponse updateSubscriptionByUserId(Long userId, String plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));
        return updateSubscriptionForUser(user, plan);
    }

    @Override
    public UserProfileResponse upgradeUserToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));

        user.setRole(Role.ADMIN);
        user.setActive(true);
        return toProfileResponse(userRepository.save(user));
    }

    @Override
    public UserProfileResponse suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(AuthMessages.ADMIN_CANNOT_BE_SUSPENDED);
        }
        if (user.isDeleted()) {
            throw new AccountDeletedException(AuthMessages.DELETED_ACCOUNT_CANNOT_BE_SUSPENDED);
        }

        user.setActive(false);
        User updatedUser = userRepository.save(user);
        invalidateOtpForUser(updatedUser);
        return toProfileResponse(updatedUser);
    }

    @Override
    public UserProfileResponse restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN && user.isDeleted()) {
            throw new IllegalArgumentException(AuthMessages.DELETED_ADMIN_CANNOT_BE_RESTORED);
        }

        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setActive(true);
        return toProfileResponse(userRepository.save(user));
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(AuthMessages.ADMIN_CANNOT_BE_DELETED);
        }

        user.setActive(false);
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        invalidateOtpForUser(user);
        emailVerificationRepository.deleteByUserId(userId);
        log.info("[AUTH] User soft-deleted by admin: userId={}, email={}", userId, user.getEmail());
    }


    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new InvalidCredentialsException(AuthMessages.EMAIL_AND_PASSWORD_REQUIRED);
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new InvalidCredentialsException(AuthMessages.INVALID_CREDENTIALS);
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    private void validateUserStatus(User user) {
        if (user.isDeleted()) {
            throw new AccountDeletedException(AuthMessages.ACCOUNT_DELETED);
        }
        if (!user.isActive()) {
            throw new AccountSuspendedException(AuthMessages.ACCOUNT_SUSPENDED);
        }
    }

    private void setupPasswordIfBlank(User user, RegisterRequest request) {
        if (isBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            if (!isBlank(request.getFullName())) {
                user.setFullName(request.getFullName());
            }
            userRepository.save(user);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException(AuthMessages.INVALID_CREDENTIALS);
        }
    }

    private User createNewUser(RegisterRequest request, String email) {
        String fullName = isBlank(request.getFullName()) ? email : request.getFullName();
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .isActive(true)
                .isVerified(false)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build();
        User savedUser = userRepository.save(user);
        log.info("[AUTH] New user registered (pending OTP): email={}, userId={}", email, savedUser.getUserId());
        return savedUser;
    }

    private AuthResponse generateLoginOtpResponse(User user, String email) {
        String fullName = user.getFullName() != null ? user.getFullName() : "User";
        OtpService.OtpGenerationResult otpResult = otpService.generateOtp(email, OtpPurpose.LOGIN, user.getUserId());
        return buildOtpPendingResponse(email, fullName, otpResult, "LOGIN");
    }

    private AuthResponse generateRegisterOtpResponse(User user, String email, String providedFullName) {
        String fullName = isBlank(providedFullName) ? email : providedFullName;
        OtpService.OtpGenerationResult otpResult = otpService.generateOtp(email, OtpPurpose.REGISTER, user.getUserId());
        return buildOtpPendingResponse(email, fullName, otpResult, "REGISTER");
    }

    private AuthResponse buildOtpPendingResponse(String email, String fullName,
            OtpService.OtpGenerationResult otpResult, String purpose) {
        if (otpResult.isCooldownActive()) {
            String message = String.format(AuthMessages.OTP_COOLDOWN_MESSAGE, otpResult.getWaitSeconds());
            throw new InvalidOtpException(message);
        }
        if (otpResult.isRateLimited()) {
            throw new InvalidOtpException(AuthMessages.OTP_RATE_LIMITED);
        }

        return AuthResponse.builder()
                .requiresOtp(true)
                .otpEmail(email)
                .userName(fullName)
                .otpPurpose(purpose)
                .email(email)
                .fullName(fullName)
                .rawOtp(otpResult.getRawOtp())
                .build();
    }

    private OtpResponse buildFailedOtpResponse(String email, String message) {
        return OtpResponse.builder()
                .success(false)
                .message(message)
                .email(email)
                .build();
    }

    private OtpResponse handleOtpGenerationResult(OtpService.OtpGenerationResult result, String email, String name) {
        if (result.isCooldownActive()) {
            String message = String.format(AuthMessages.OTP_COOLDOWN_MESSAGE, result.getWaitSeconds());
            return OtpResponse.builder()
                    .success(false)
                    .message(message)
                    .email(email)
                    .resendCooldownSeconds(result.getWaitSeconds())
                    .build();
        }

        if (result.isRateLimited()) {
            return buildFailedOtpResponse(email, AuthMessages.OTP_RATE_LIMITED);
        }

        return OtpResponse.builder()
                .success(true)
                .message(AuthMessages.OTP_GENERATED_SUCCESSFULLY)
                .email(email)
                .name(name)
                .expiresInSeconds(result.getExpiresInSeconds())
                .resendCooldownSeconds(30)
                .rawOtp(result.getRawOtp())
                .build();
    }

    private void markUserAsVerified(User user, String email) {
        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
            log.info("[AUTH] User verified via OTP: email={}", email);
        }
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .subscriptionPlan(user.getSubscriptionPlan().name())
                .requiresOtp(false)
                .build();
    }

    private OtpPurpose parseOtpPurpose(String purposeStr) {
        if (isBlank(purposeStr)) {
            return OtpPurpose.LOGIN;
        }
        try {
            return OtpPurpose.valueOf(purposeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OtpPurpose.LOGIN;
        }
    }

    private User getActiveUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(AuthMessages.USER_NOT_FOUND));
        validateUserStatus(user);
        return user;
    }

    private void invalidateOtpForUser(User user) {
        if (user == null || isBlank(user.getEmail())) {
            return;
        }
        String email = user.getEmail().trim().toLowerCase();
        for (OtpPurpose purpose : OtpPurpose.values()) {
            emailVerificationRepository.invalidateAllActiveOtps(email, purpose);
        }
    }

    private UserProfileResponse updateSubscriptionForUser(User user, String plan) {
        try {
            user.setSubscriptionPlan(SubscriptionPlan.valueOf(plan.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(AuthMessages.INVALID_SUBSCRIPTION_PLAN);
        }
        return toProfileResponse(userRepository.save(user));
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
                .isDeleted(user.isDeleted())
                .subscriptionPlan(user.getSubscriptionPlan().name())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}



