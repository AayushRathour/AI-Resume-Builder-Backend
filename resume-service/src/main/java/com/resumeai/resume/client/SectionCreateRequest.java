package com.resumeai.resume.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload to create a new section in section-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionCreateRequest {

    private Long resumeId;
    private String sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
    private Boolean aiGenerated;
}
