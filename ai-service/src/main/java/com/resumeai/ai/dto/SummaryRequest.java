package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for summary operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SummaryRequest {
    private String jobTitle;
    private String yearsOfExperience;
    private String keySkills;
    private String additionalContext;
}



