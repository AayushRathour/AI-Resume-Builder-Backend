package com.resumeai.template.dto;

import java.time.LocalDateTime;

import com.resumeai.template.entity.TemplateCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response payload for template operations. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

    private Long templateId;
    private String name;
    private TemplateCategory category;
    private String description;
    private String htmlContent;
    private String cssContent;
    private String fieldsJson;
    private String previewImageUrl;
    private boolean isPremium;
    private boolean isActive;
    private Long usageCount;
    private LocalDateTime createdAt;
}



