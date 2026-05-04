package com.resumeai.jobmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingSkillsResponse {
    private String missingSkills;
    private String recommendations;
}
