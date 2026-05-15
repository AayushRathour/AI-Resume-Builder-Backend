package com.resumeai.auth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for user profile operations. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String provider;
    private boolean isActive;
    private boolean isDeleted;
    private String subscriptionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}



