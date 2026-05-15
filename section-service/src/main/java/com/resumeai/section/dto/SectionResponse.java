package com.resumeai.section.dto;

import java.time.LocalDateTime;

import com.resumeai.section.entity.SectionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for section operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {

    private Long sectionId;
    private Long resumeId;
    private SectionType sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



