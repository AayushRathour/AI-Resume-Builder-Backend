package com.resumeai.jobmatch.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeExtractResponse {
    private List<String> skills;
    private List<String> roles;
    private String experience;
    private List<String> keywords;
}
