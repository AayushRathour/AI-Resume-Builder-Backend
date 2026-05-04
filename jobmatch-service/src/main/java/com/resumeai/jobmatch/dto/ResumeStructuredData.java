package com.resumeai.jobmatch.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStructuredData {

    private String name;

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Builder.Default
    private List<String> experience = new ArrayList<>();

    @Builder.Default
    private List<String> education = new ArrayList<>();

    private String summary;
    private String normalizedText;
}
