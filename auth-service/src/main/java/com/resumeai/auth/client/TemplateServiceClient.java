package com.resumeai.auth.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

/** Feign client for synchronous calls to template service APIs. */
@FeignClient(name = "template-service", fallback = TemplateServiceClientFallback.class)
public interface TemplateServiceClient {

    @GetMapping("/api/templates")
    List<Map<String, Object>> getTemplates();

    @PostMapping("/api/templates")
    Map<String, Object> createTemplate(@RequestBody Map<String, Object> payload);

    @PutMapping("/api/templates/{id}")
    Map<String, Object> updateTemplate(@PathVariable("id") Long id, @RequestBody Map<String, Object> payload);

    default List<Map<String, Object>> requireTemplates() {
        List<Map<String, Object>> templates = getTemplates();
        if (templates == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Template service unavailable");
        }
        return templates;
    }
}




