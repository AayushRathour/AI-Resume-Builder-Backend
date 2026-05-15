package com.resumeai.jobmatch.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/** Response payload for analysis operations. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    
    private ExtractedData extractedData;
    private List<MatchResponse> matches;
    private List<Map<String, Object>> jobs;
    private int totalMatches;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtractedData {
        private List<String> skills;
        private List<String> roles;
        private List<String> keywords;
        private String experienceLevel;
        private String summary;
    }
}



