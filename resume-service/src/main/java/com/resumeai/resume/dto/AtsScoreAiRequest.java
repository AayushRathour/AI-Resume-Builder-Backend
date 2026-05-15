package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for AI-based ATS scoring.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsScoreAiRequest {

    @NotBlank(message = "Resume text is required")
    private String resumeText;

    private String jobDescription;
}
