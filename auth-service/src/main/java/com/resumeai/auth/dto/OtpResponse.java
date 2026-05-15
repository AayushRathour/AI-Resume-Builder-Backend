package com.resumeai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for OTP operations. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private boolean success;
    private String message;
    private String email;
    private String name;
    private int expiresInSeconds;
    private int resendCooldownSeconds;
    private String rawOtp; // Raw OTP for client-side EmailJS sending (only on generate/resend)

    // Included only on successful verification
    private AuthResponse authResponse;
}



