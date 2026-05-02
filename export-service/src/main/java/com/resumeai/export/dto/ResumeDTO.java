package com.resumeai.export.dto;

import lombok.*;

/**
 * Mirrors resume-service ResumeResponse.
 * Field names must match JSON keys returned by GET /api/resumes/{id}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDTO {

    private Long resumeId;
    private Long userId;
    /** Resume document title (e.g. "Software Engineer Resume 2025") */
    private String title;
    private String targetJobTitle;
    private Long templateId;
    private String language;
    private String sectionsJson;
    private Double atsScore;
    private String status;
}
