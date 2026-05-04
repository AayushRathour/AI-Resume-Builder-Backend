package com.resumeai.ai.dto;

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
public class MissingSkillsRequest {
    private Long userId;
    private Long resumeId;
    private String resumeText;
    private String jobDescription;
}
