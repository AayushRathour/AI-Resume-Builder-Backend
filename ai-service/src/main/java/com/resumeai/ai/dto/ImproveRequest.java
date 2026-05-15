package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for improve operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImproveRequest {
    private String sectionType;
    private String currentContent;
    private String targetRole;
}



