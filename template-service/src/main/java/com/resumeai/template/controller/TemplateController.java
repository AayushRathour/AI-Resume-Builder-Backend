package com.resumeai.template.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.template.dto.TemplateRequest;
import com.resumeai.template.dto.TemplateResponse;
import com.resumeai.template.service.TemplateService;

import lombok.RequiredArgsConstructor;

/** Exposes REST endpoints for template workflows. */

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable("id") Long templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @GetMapping("/{id}/fields")
    public ResponseEntity<String> getTemplateFields(@PathVariable("id") Long templateId) {
        try {
            TemplateResponse template = templateService.getTemplateById(templateId);
            String fields = template.getFieldsJson();
            if (fields != null && !fields.isBlank()) {
                return ResponseEntity.ok(fields);
            }
            // Default fallback if template has no fieldsJson set
            return ResponseEntity.ok(
                "[{\"key\":\"name\",\"label\":\"Full Name\"}," +
                "{\"key\":\"title\",\"label\":\"Job Title\"}," +
                "{\"key\":\"email\",\"label\":\"Email\"}," +
                "{\"key\":\"phone\",\"label\":\"Phone Number\"}," +
                "{\"key\":\"linkedin\",\"label\":\"LinkedIn Profile\"}," +
                "{\"key\":\"summary\",\"label\":\"Professional Summary\"}," +
                "{\"key\":\"skills\",\"label\":\"Skills\"}," +
                "{\"key\":\"experience\",\"label\":\"Experience\"}," +
                "{\"key\":\"education\",\"label\":\"Education\"}]"
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable("id") Long templateId,
            @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") Long templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/free")
    public ResponseEntity<List<TemplateResponse>> getFreeTemplates() {
        return ResponseEntity.ok(templateService.getFreeTemplates());
    }

    @GetMapping("/premium")
    public ResponseEntity<List<TemplateResponse>> getPremiumTemplates() {
        return ResponseEntity.ok(templateService.getPremiumTemplates());
    }
}



