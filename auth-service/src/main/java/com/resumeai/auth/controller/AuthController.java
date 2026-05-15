package com.resumeai.auth.controller;

import com.resumeai.auth.constants.AuthMessages;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.MessageResponse;
import com.resumeai.auth.dto.OtpRequest;
import com.resumeai.auth.dto.OtpResponse;
import com.resumeai.auth.dto.OtpVerifyRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.SubscriptionUpdateRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exposes REST endpoints for authentication workflows. */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user and starts the OTP verification flow.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Authenticates credentials and returns JWT or OTP challenge.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


    /**
     * Sends a new OTP for the requested purpose (login/register/reset).
     */
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@RequestBody OtpRequest request) {
        log.info("[OTP] Send OTP request for email={}, purpose={}", request.getEmail(), request.getPurpose());
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false).message(AuthMessages.EMAIL_REQUIRED).build());
        }
        // Trigger OTP generation with cooldown/rate-limit handling.
        OtpResponse response = authService.sendOtp(request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies OTP and returns JWT on success.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        log.info("[OTP] Verify OTP request for email={}, purpose={}", request.getEmail(), request.getPurpose());
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getOtp() == null || request.getOtp().isBlank()) {
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false).message(AuthMessages.EMAIL_AND_OTP_REQUIRED).build());
        }
        // Validate OTP and issue token if verification succeeds.
        OtpResponse response = authService.verifyOtp(request.getEmail(), request.getOtp(), request.getPurpose());
        return ResponseEntity.ok(response);
    }

    /**
     * Resends OTP for the requested purpose.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@RequestBody OtpRequest request) {
        log.info("[OTP] Resend OTP request for email={}, purpose={}", request.getEmail(), request.getPurpose());
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false).message(AuthMessages.EMAIL_REQUIRED).build());
        }
        OtpResponse response = authService.resendOtp(request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(response);
    }


    /**
     * Returns the current user's profile based on JWT principal.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        ensureAuthenticated(authentication);
        return ResponseEntity.ok(authService.getProfile(authentication.getName()));
    }

    /**
     * Updates profile fields for the authenticated user.
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
            @RequestBody RegisterRequest request) {
        ensureAuthenticated(authentication);
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    /**
     * Changes account password after verifying the old password.
     */
    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        ensureAuthenticated(authentication);
        authService.changePassword(authentication.getName(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse(AuthMessages.PASSWORD_UPDATED_SUCCESSFULLY));
    }

    /**
     * Updates the user's subscription plan and returns the refreshed profile.
     */
    @PutMapping("/subscription")
    public ResponseEntity<UserProfileResponse> updateSubscription(Authentication authentication,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        ensureAuthenticated(authentication);
        return ResponseEntity.ok(authService.updateSubscription(authentication.getName(), request.getPlan()));
    }

    /**
     * Deactivates the authenticated account (soft disable).
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<MessageResponse> deactivateAccount(Authentication authentication) {
        ensureAuthenticated(authentication);
        authService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(new MessageResponse(AuthMessages.ACCOUNT_DEACTIVATED_SUCCESSFULLY));
    }

    /**
     * Admin: returns all users with profile details.
     */
    @GetMapping("/admin/users")
    public ResponseEntity<java.util.List<UserProfileResponse>> getAllUsers(Authentication authentication) {
        ensureAdmin(authentication);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * Admin: deletes a user by id (soft delete).
     */
    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(Authentication authentication,
            @PathVariable Long userId) {
        ensureAdmin(authentication);
        authService.deleteUserById(userId);
        return ResponseEntity.ok(new MessageResponse(AuthMessages.USER_DELETED_SUCCESSFULLY));
    }

    /**
     * Admin: updates subscription plan for a specific user.
     */
    @PutMapping("/admin/users/{userId}/subscription")
    public ResponseEntity<UserProfileResponse> updateUserSubscription(Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        ensureAdmin(authentication);
        return ResponseEntity.ok(authService.updateSubscriptionByUserId(userId, request.getPlan()));
    }


    private void ensureAuthenticated(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AuthMessages.AUTHENTICATION_REQUIRED);
        }
    }

    private void ensureAdmin(Authentication authentication) {
        ensureAuthenticated(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AuthMessages.ADMIN_ACCESS_REQUIRED);
        }
    }
}



