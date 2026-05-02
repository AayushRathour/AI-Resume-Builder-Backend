package com.resumeai.export.client;

import com.resumeai.export.dto.SectionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

/**
 * WebClient-based client for section-service.
 * Uses service name (http://section-service) resolved via Eureka load balancer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SectionClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.section.url}")
    private String sectionServiceUrl;

    public List<SectionDTO> getSectionsByResumeId(Long resumeId, Long userId) {
        log.debug("Fetching sections for resumeId={} from section-service for user={}", resumeId, userId);
        try {
            List<SectionDTO> sections = webClientBuilder.build()
                    .get()
                    .uri(sectionServiceUrl + "/api/sections/resume/" + resumeId)
                    .header("X-User-Id", userId != null ? String.valueOf(userId) : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<SectionDTO>>() {})
                    .block();
            return sections != null ? sections : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Could not fetch sections for resumeId={}: {}", resumeId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
