package com.resumeai.export.dto;

import lombok.*;

/**
 * Mirrors template-service TemplateResponse.
 * Field names must match JSON keys returned by GET /api/templates/{id}.
 *
 * NOTE: template-service does not expose htmlContent or cssContent.
 * ExportServiceImpl falls back to built-in default CSS when this DTO
 * has no styling information.
 */
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
