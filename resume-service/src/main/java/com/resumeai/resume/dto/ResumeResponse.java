package com.resumeai.resume.dto;

import java.time.LocalDateTime;

import com.resumeai.resume.entity.ResumeStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload representing resume details and status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {

    private Long resumeId;
    private Long userId;
    private String name;
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
    private ResumeStatus status;
    private Boolean isPublic;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
