package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ResumeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Target job title is required")
    private String targetJobTitle;

    @NotNull(message = "Template ID is required")
    @Positive(message = "Template ID must be a positive number")
    private Long templateId;

    // language is optional — defaults to "English" in the service layer
    private String language;

    private String sectionsJson;
}
