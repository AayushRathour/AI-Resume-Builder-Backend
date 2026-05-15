package com.resumeai.template.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resumeai.template.dto.TemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.service.TemplateService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController templateController;

    private TemplateResponse responseDto;

    @BeforeEach
    void setUp() {
        responseDto = TemplateResponse.builder().templateId(1L).fieldsJson("[{}]").build();
    }

    @Test
    void createTemplate() {
        TemplateRequest req = new TemplateRequest();
        when(templateService.createTemplate(req)).thenReturn(responseDto);

        ResponseEntity<TemplateResponse> response = templateController.createTemplate(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void getAllTemplates() {
        when(templateService.getAllTemplates()).thenReturn(List.of(responseDto));
        ResponseEntity<List<TemplateResponse>> response = templateController.getAllTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getTemplateById() {
        when(templateService.getTemplateById(1L)).thenReturn(responseDto);
        ResponseEntity<TemplateResponse> response = templateController.getTemplateById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void getTemplateFields_success() {
        when(templateService.getTemplateById(1L)).thenReturn(responseDto);
        ResponseEntity<String> response = templateController.getTemplateFields(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[{}]", response.getBody());
    }

    @Test
    void getTemplateFields_fallback() {
        responseDto.setFieldsJson(null);
        when(templateService.getTemplateById(1L)).thenReturn(responseDto);
        ResponseEntity<String> response = templateController.getTemplateFields(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTemplateFields_notFound() {
        when(templateService.getTemplateById(1L)).thenThrow(new RuntimeException());
        ResponseEntity<String> response = templateController.getTemplateFields(1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateTemplate() {
        TemplateRequest req = new TemplateRequest();
        when(templateService.updateTemplate(1L, req)).thenReturn(responseDto);
        ResponseEntity<TemplateResponse> response = templateController.updateTemplate(1L, req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteTemplate() {
        ResponseEntity<Void> response = templateController.deleteTemplate(1L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(templateService).deleteTemplate(1L);
    }

    @Test
    void getFreeTemplates() {
        when(templateService.getFreeTemplates()).thenReturn(List.of(responseDto));
        ResponseEntity<List<TemplateResponse>> response = templateController.getFreeTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPremiumTemplates() {
        when(templateService.getPremiumTemplates()).thenReturn(List.of(responseDto));
        ResponseEntity<List<TemplateResponse>> response = templateController.getPremiumTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
