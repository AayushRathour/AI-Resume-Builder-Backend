package com.resumeai.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Summary response for ATS backfill operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsBackfillResponse {
    private Long userId;
    private int scanned;
    private int eligible;
    private int processed;
    private int updated;
    private int failed;
}

