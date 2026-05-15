package com.resumeai.jobmatch.dto;

import lombok.*;

/** DTO for structured resume data exchange across services. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDTO {

    private Long resumeId;
    private Long userId;
    private String title;
    private String targetJobTitle;
    private String sectionsJson;
}



