package com.resumeai.jobmatch.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiExtractionResponse {
    private List<String> skills;
    private List<String> roles;

    @JsonProperty("experience_level")
    @JsonAlias({"experienceLevel"})
    private String experienceLevel;

    private List<String> keywords;
}
