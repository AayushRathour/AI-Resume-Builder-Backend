package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoverLetterRequest {
    private String jobTitle;
    private String companyName;
    private String jobDescription;
    private String applicantSummary;
}
