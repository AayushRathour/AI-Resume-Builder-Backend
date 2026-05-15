package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for translate operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TranslateRequest {
    private String resumeContent;
    private String targetLanguage;
}



