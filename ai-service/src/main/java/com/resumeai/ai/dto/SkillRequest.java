package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for skill operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillRequest {
    private String jobTitle;
    private String currentSkills;
    private String industry;
}



