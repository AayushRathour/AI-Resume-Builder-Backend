package com.resumeai.resume.client;

import lombok.Getter;
import lombok.Setter;

/**
 * Section projection used when reading sections from section-service.
 */
@Getter
@Setter
public class SectionPayload {

    private Long sectionId;
    private Long resumeId;
    private String sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
    private Boolean aiGenerated;
}
