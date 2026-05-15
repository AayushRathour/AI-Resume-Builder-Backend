package com.resumeai.auth.service;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.OtpResponse;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import java.util.List;

/** Defines authentication service operations exposed to controllers. */
public interface AuthService {

    /**
     * Registers a new user and initiates OTP verification.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates user credentials and returns JWT or OTP challenge.
     */
    AuthResponse login(LoginRequest request);

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, RegisterRequest request);

    void changePassword(String email, String oldPassword, String newPassword);

    void deactivateAccount(String email);

    /**
     * Updates the subscription plan for the authenticated user.
     */
    UserProfileResponse updateSubscription(String email, String plan);

    UserProfileResponse updateSubscriptionByUserId(Long userId, String plan);

    UserProfileResponse upgradeUserToAdmin(Long userId);

    UserProfileResponse suspendUser(Long userId);

    UserProfileResponse restoreUser(Long userId);

    List<UserProfileResponse> getAllUsers();

    void deleteUserById(Long userId);


    /**
     * Generates and sends OTP for a given purpose.
     */
    OtpResponse sendOtp(String email, String purpose);

    /**
     * Verifies OTP and issues JWT when successful.
     */
    OtpResponse verifyOtp(String email, String otp, String purpose);

    /**
     * Resends OTP with cooldown and rate limiting enforcement.
     */
    OtpResponse resendOtp(String email, String purpose);
}

