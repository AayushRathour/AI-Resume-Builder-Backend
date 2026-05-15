package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for tailor operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TailorRequest {
    private String resumeContent;
    private String jobDescription;
    private String jobTitle;
}



