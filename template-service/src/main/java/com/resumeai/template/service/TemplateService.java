package com.resumeai.template.service;

import java.util.List;

import com.resumeai.template.dto.TemplateRequest;
import com.resumeai.template.dto.TemplateResponse;

/** Defines service operations for template workflows. */

public interface TemplateService {

    TemplateResponse createTemplate(TemplateRequest request);

    List<TemplateResponse> getAllTemplates();

    TemplateResponse getTemplateById(Long templateId);

    TemplateResponse updateTemplate(Long templateId, TemplateRequest request);

    void deleteTemplate(Long templateId);

    List<TemplateResponse> getPremiumTemplates();

    List<TemplateResponse> getFreeTemplates();
}

