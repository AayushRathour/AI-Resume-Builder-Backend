package com.resumeai.resume.dto;

import java.time.LocalDateTime;

import com.resumeai.resume.entity.ResumeStatus;

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
public class ResumeResponse {

    private Long resumeId;
    private Long userId;
    private String title;
    private String targetJobTitle;
    private Long templateId;
    private String language;
    private String sectionsJson;
    private Double atsScore;
    private ResumeStatus status;
    private Boolean isPublic;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
