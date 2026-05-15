package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for missing skills operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingSkillsResponse {
    private String missingSkills;
    private String recommendations;
}



