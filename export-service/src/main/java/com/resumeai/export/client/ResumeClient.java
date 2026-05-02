package com.resumeai.export.client;

import com.resumeai.export.dto.ResumeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient-based client for resume-service.
 * Uses service name (http://resume-service) resolved via Eureka load balancer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.resume.url}")
    private String resumeServiceUrl;

    public ResumeDTO getResumeById(Long resumeId, Long userId) {
        log.debug("Fetching resume id={} from resume-service for user={}", resumeId, userId);
        return webClientBuilder.build()
                .get()
                .uri(resumeServiceUrl + "/api/resumes/" + resumeId)
                .header("X-User-Id", userId != null ? String.valueOf(userId) : "")
                .retrieve()
                .bodyToMono(ResumeDTO.class)
                .block();
    }
}
