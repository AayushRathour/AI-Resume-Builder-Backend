package com.resumeai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for OTP operations. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private String email;
    private String purpose; // REGISTER, LOGIN, RESET_PASSWORD
}



