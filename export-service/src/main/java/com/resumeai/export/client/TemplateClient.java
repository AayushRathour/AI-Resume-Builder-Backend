package com.resumeai.export.client;

import com.resumeai.export.dto.TemplateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "template-service",
        fallback = TemplateClientFallback.class
)

/** Feign client for synchronous calls to template APIs. */
public interface TemplateClient {

    @GetMapping("/api/templates/{templateId}")
    TemplateDTO getTemplateById(@PathVariable("templateId") Long templateId);
}



