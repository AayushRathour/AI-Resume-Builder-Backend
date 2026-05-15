package com.resumeai.auth.client;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for template service call failures. */
@Component
@Slf4j
public class TemplateServiceClientFallback implements TemplateServiceClient {

    @Override
    public List<Map<String, Object>> getTemplates() {
        log.warn("TemplateServiceClient fallback for getTemplates");
        return List.of();
    }

    @Override
    public Map<String, Object> createTemplate(Map<String, Object> payload) {
        log.warn("TemplateServiceClient fallback for createTemplate");
        return Map.of("status", "failed", "message", "Template service unavailable");
    }

    @Override
    public Map<String, Object> updateTemplate(Long id, Map<String, Object> payload) {
        log.warn("TemplateServiceClient fallback for updateTemplate id={}", id);
        return Map.of("status", "failed", "message", "Template service unavailable");
    }
}





