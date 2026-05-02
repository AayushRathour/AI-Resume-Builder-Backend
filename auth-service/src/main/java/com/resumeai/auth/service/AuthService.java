package com.resumeai.auth.service;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, RegisterRequest request);

    void changePassword(String email, String oldPassword, String newPassword);

    void deactivateAccount(String email);

    UserProfileResponse updateSubscription(String email, String plan);

    UserProfileResponse updateSubscriptionByUserId(Long userId, String plan);

    UserProfileResponse upgradeUserToAdmin(Long userId);

    UserProfileResponse suspendUser(Long userId);

    List<UserProfileResponse> getAllUsers();

    void deleteUserById(Long userId);
}
