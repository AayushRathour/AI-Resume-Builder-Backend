package com.resumeai.jobmatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionDTO {

    private Long sectionId;
    private Long resumeId;
    private String sectionType;
    private String title;
    private String content;
    private Integer orderIndex;
    private Boolean isVisible;
}
