package com.resumeai.export.dto;

import lombok.*;

/** DTO for structured template data exchange across services. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDTO {

    private Long templateId;
    private String name;
    private String description;
    private String htmlContent;
    private String cssContent;
    private String fieldsJson;
    private String previewImageUrl;
    private Boolean isPremium;
}



