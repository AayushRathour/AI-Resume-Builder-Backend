package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
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

    private String name;

    private String email;

    private String phone;

    private String location;

    @NotBlank(message = "Target job title is required")
    private String targetJobTitle;

    private Long templateId;

    private String summary;

    private String skills;

    private String experience;

    private String education;

    private String projects;

    // language is optional — defaults to "English" in the service layer
    private String language;

    private String sectionsJson;
}
