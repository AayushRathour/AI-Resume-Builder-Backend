package com.resumeai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for authentication operations. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String subscriptionPlan;

    // OTP flow fields
    @Builder.Default
    private boolean requiresOtp = false;
    private String otpEmail;
    private String userName;
    private String otpPurpose;
    private String rawOtp; // Only set during register/login for client-side EmailJS sending
}



