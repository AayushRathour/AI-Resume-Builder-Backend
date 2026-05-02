package com.resumeai.export.dto;

import lombok.*;

/**
 * Mirrors section-service SectionResponse.
 * Field names must match JSON keys returned by GET /api/sections/resume/{resumeId}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionDTO {

    private Long sectionId;
    private Long resumeId;
    /** Serialised SectionType enum string (e.g. "EXPERIENCE", "SKILLS"). */
    private String sectionType;
    private String title;
    private String content;
    private Integer orderIndex;
    private Boolean isVisible;
}
