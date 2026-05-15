package com.resumeai.export.dto;

import lombok.*;

/** DTO for structured section data exchange across services. */
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



