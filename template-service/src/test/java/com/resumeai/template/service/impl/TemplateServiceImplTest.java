package com.resumeai.template.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.resumeai.template.dto.TemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.entity.Template;
import com.resumeai.template.exception.TemplateNotFoundException;
import com.resumeai.template.repository.TemplateRepository;
import com.resumeai.template.service.NotificationProducer;
import com.resumeai.template.service.TemplateValidationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private TemplateValidationService templateValidationService;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private Template template;
    private TemplateRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(templateValidationService.sanitizeHtml(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        template = Template.builder()
                .templateId(1L)
                .name("Modern Template")
                .isPremium(true)
                .fieldsJson("[]")
                .htmlContent("<html/>")
                .previewImageUrl("http://img.com/1.png")
                .build();

        request = TemplateRequest.builder()
                .name("Modern Template Updated")
                .isPremium(false)
                .fieldsJson("[{}]")
                .htmlContent("<html></html>")
                .previewImageUrl("http://img.com/2.png")
                .build();
    }

    @Test
    void createTemplate_success() {
        when(templateRepository.save(any(Template.class))).thenReturn(template);

        TemplateResponse response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals(1L, response.getTemplateId());
        verify(notificationProducer).publishTemplateCreatedEvent(1L, "Modern Template");
    }

    @Test
    void getTemplateById_success() {
        when(templateRepository.findByTemplateId(1L)).thenReturn(Optional.of(template));

        TemplateResponse response = templateService.getTemplateById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getTemplateId());
    }

    @Test
    void getTemplateById_notFound() {
        when(templateRepository.findByTemplateId(1L)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> templateService.getTemplateById(1L));
    }

    @Test
    void getAllTemplates_success() {
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(template));

        List<TemplateResponse> templates = templateService.getAllTemplates();

        assertFalse(templates.isEmpty());
        assertEquals(1, templates.size());
    }

    @Test
    void getFreeTemplates_success() {
        when(templateRepository.findByIsActiveTrueAndIsPremium(false)).thenReturn(List.of(template));

        List<TemplateResponse> templates = templateService.getFreeTemplates();

        assertEquals(1, templates.size());
    }

    @Test
    void getPremiumTemplates_success() {
        when(templateRepository.findByIsActiveTrueAndIsPremium(true)).thenReturn(List.of(template));

        List<TemplateResponse> templates = templateService.getPremiumTemplates();

        assertEquals(1, templates.size());
    }

    @Test
    void updateTemplate_success() {
        when(templateRepository.findByTemplateId(1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(Template.class))).thenReturn(template);

        TemplateResponse response = templateService.updateTemplate(1L, request);

        assertNotNull(response);
        verify(notificationProducer).publishTemplateUpdatedEvent(1L, template.getName());
    }

    @Test
    void deleteTemplate_success() {
        when(templateRepository.findByTemplateId(1L)).thenReturn(Optional.of(template));

        templateService.deleteTemplate(1L);

        verify(templateRepository).deleteByTemplateId(1L);
        verify(notificationProducer).publishTemplateDeletedEvent(1L);
    }
}
