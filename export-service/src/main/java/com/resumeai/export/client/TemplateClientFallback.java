package com.resumeai.export.client;

import com.resumeai.export.dto.TemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for template service call failures. */

@Slf4j
@Component
public class TemplateClientFallback implements TemplateClient {

    @Override
    public TemplateDTO getTemplateById(Long templateId) {
        log.warn("TemplateClient fallback triggered for templateId={}", templateId);
        throw new IllegalStateException(
                "template-service is currently unavailable; unable to fetch templateId=" + templateId);
    }
}



