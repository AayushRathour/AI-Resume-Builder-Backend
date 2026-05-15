package com.resumeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for AI operations. */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AIResponse {
    private String text;
    private String model;
    private Integer tokensUsed;
    private String requestId;
}



