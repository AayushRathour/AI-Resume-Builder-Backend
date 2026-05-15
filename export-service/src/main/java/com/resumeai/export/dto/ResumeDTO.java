package com.resumeai.export.dto;

import lombok.*;

/** DTO for structured resume data exchange across services. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDTO {

    private Long resumeId;
    private Long userId;
    private String name;
    /** Resume document title (e.g. "Software Engineer Resume 2025") */
    private String title;
    private String email;
    private String phone;
    private String location;
    private String targetJobTitle;
    private Long templateId;
    private String language;
    private String summary;
    private String skills;
    private String experience;
    private String education;
    private String projects;
    private String sectionsJson;
    private Double atsScore;
    private String status;
}



