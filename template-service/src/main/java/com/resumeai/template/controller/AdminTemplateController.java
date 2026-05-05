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

@RestController
@RequestMapping("/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.createTemplate(request);
        System.out.println("ADMIN SAVED TEMPLATE: " + response.getTemplateId());
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

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable("id") Long templateId,
            @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.updateTemplate(templateId, request);
        System.out.println("ADMIN UPDATED TEMPLATE: " + response.getTemplateId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") Long templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
