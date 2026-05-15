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
import com.resumeai.template.service.NotificationProducer;
import com.resumeai.template.service.TemplateService;
import com.resumeai.template.service.TemplateValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Implements template workflows and service-layer orchestration. */

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final NotificationProducer notificationProducer;
    private final TemplateValidationService templateValidationService;

    @Override
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        // Sanitize and validate template markup before storing or exposing it.
        String sanitizedHtml = templateValidationService.sanitizeHtml(request.getHtmlContent());
        templateValidationService.validateHtml(sanitizedHtml);

        Template template = Template.builder()
                .name(request.getName())
                .category(request.getCategory() == null ? TemplateCategory.PROFESSIONAL : request.getCategory())
                .description(request.getDescription())
            .htmlContent(sanitizedHtml)
                .cssContent(request.getCssContent())
                .fieldsJson(request.getFieldsJson())
                .previewImageUrl(request.getPreviewImageUrl())
                .isPremium(request.isPremium())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        Template saved = templateRepository.save(template);

        // Publish template.created event
        try {
            notificationProducer.publishTemplateCreatedEvent(saved.getTemplateId(), saved.getName());
        } catch (Exception e) {
            log.warn("Failed to publish template.created event: {}", e.getMessage());
        }

        return mapToResponse(saved);
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

        // Re-validate edited HTML to keep render output safe and predictable.
        String sanitizedHtml = templateValidationService.sanitizeHtml(request.getHtmlContent());
        templateValidationService.validateHtml(sanitizedHtml);

        template.setName(request.getName());
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        template.setDescription(request.getDescription());
        template.setHtmlContent(sanitizedHtml);
        template.setCssContent(request.getCssContent());
        if (request.getFieldsJson() != null) {
            template.setFieldsJson(request.getFieldsJson());
        }
        template.setPreviewImageUrl(request.getPreviewImageUrl());
        template.setPremium(request.isPremium());
        if (request.getIsActive() != null) {
            template.setActive(request.getIsActive());
        }

        Template saved = templateRepository.save(template);

        // Publish template.updated event
        try {
            notificationProducer.publishTemplateUpdatedEvent(saved.getTemplateId(), saved.getName());
        } catch (Exception e) {
            log.warn("Failed to publish template.updated event: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        getTemplateEntityById(templateId);
        templateRepository.deleteByTemplateId(templateId);

        // Publish template.deleted event
        try {
            notificationProducer.publishTemplateDeletedEvent(templateId);
        } catch (Exception e) {
            log.warn("Failed to publish template.deleted event: {}", e.getMessage());
        }
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
        String safeHtml = templateValidationService.sanitizeHtml(template.getHtmlContent());
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .name(template.getName())
                .category(template.getCategory())
                .description(template.getDescription())
                .htmlContent(safeHtml)
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



