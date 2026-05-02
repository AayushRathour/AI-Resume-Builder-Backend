package com.resumeai.jobmatch.dto;

import lombok.*;

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
