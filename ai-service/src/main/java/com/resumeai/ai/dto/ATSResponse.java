package com.resumeai.ai.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ATSResponse {
    private int score;
    private List<String> missingKeywords;
    private String recommendations;
    private String requestId;
}
