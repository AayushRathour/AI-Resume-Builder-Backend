package com.resumeai.template.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resumeai.template.dto.TemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.entity.Template;
import com.resumeai.template.entity.TemplateCategory;
import com.resumeai.template.exception.TemplateNotFoundException;
import com.resumeai.template.repository.TemplateRepository;
import com.resumeai.template.service.TemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        Template template = Template.builder()
                .name(request.getName())
                .category(request.getCategory() == null ? TemplateCategory.PROFESSIONAL : request.getCategory())
                .description(request.getDescription())
                .htmlContent(request.getHtmlContent())
                .cssContent(request.getCssContent())
                .fieldsJson(request.getFieldsJson())
                .previewImageUrl(request.getPreviewImageUrl())
                .isPremium(request.isPremium())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(Long templateId) {
        Template template = getTemplateEntityById(templateId);
        return mapToResponse(template);
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(Long templateId, TemplateRequest request) {
        Template template = getTemplateEntityById(templateId);

        template.setName(request.getName());
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        template.setDescription(request.getDescription());
        template.setHtmlContent(request.getHtmlContent());
        template.setCssContent(request.getCssContent());
        if (request.getFieldsJson() != null) {
            template.setFieldsJson(request.getFieldsJson());
        }
        template.setPreviewImageUrl(request.getPreviewImageUrl());
        template.setPremium(request.isPremium());
        if (request.getIsActive() != null) {
            template.setActive(request.getIsActive());
        }

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        getTemplateEntityById(templateId);
        templateRepository.deleteByTemplateId(templateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getPremiumTemplates() {
        return templateRepository.findByIsActiveTrueAndIsPremium(true)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getFreeTemplates() {
        return templateRepository.findByIsActiveTrueAndIsPremium(false)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Template getTemplateEntityById(Long templateId) {
        return templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with id: " + templateId));
    }

    private TemplateResponse mapToResponse(Template template) {
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .name(template.getName())
                .category(template.getCategory())
                .description(template.getDescription())
                .htmlContent(template.getHtmlContent())
                .cssContent(template.getCssContent())
                .fieldsJson(template.getFieldsJson())
                .previewImageUrl(template.getPreviewImageUrl())
                .isPremium(template.isPremium())
                .isActive(template.isActive())
                .usageCount(template.getUsageCount())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
