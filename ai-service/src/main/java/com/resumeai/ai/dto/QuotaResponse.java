package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for quota operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotaResponse {
    private Long userId;
    private boolean isPremium;
    private long callsUsedThisMonth;
    private long callsAllowed;
    private long atsChecksUsedThisMonth;
    private long atsChecksAllowed;
    private long remainingCalls;
    private long remainingAtsChecks;
}



