package com.resumeai.jobmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for resume extract operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeExtractRequest {
    private Long userId;
    private Long resumeId;
    private String resumeText;
}



