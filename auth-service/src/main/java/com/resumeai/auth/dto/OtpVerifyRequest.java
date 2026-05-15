package com.resumeai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for otp verify operations. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String email;
    private String otp;
    private String purpose; // REGISTER, LOGIN, RESET_PASSWORD
}



