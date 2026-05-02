package com.resumeai.export.client;

import com.resumeai.export.dto.TemplateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient-based client for template-service.
 * Uses service name (http://template-service) resolved via Eureka load balancer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.template.url}")
    private String templateServiceUrl;

    public TemplateDTO getTemplateById(Long templateId) {
        log.debug("Fetching template id={} from template-service", templateId);
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(templateServiceUrl + "/api/templates/" + templateId)
                    .retrieve()
                    .bodyToMono(TemplateDTO.class)
                    .block();
        } catch (Exception ex) {
            log.warn("Could not fetch template id={}: {}", templateId, ex.getMessage());
            return null;
        }
    }
}
